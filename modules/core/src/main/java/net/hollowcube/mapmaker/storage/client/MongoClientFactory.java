package net.hollowcube.mapmaker.storage.client;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.hollowcube.common.config.MongoConfig;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ForkJoinPool;

public interface MongoClientFactory {

    /** Creates a new MongoClient from the given config. */
    @NotNull ListenableFuture<@NotNull MongoClient> newClient(@NotNull MongoConfig config);


    /** Returns the first registered factory, or the default factory if none are present. */
    static @NotNull MongoClientFactory get() {
        class Holder {
            static final System.Logger logger = System.getLogger(MongoClientFactory.class.getName());
            static MongoClientFactory instance = null;
        }
        if (Holder.instance == null) {
            Holder.instance = ServiceLoader.load(MongoClientFactory.class).findFirst()
                    .orElseGet(() -> config -> Futures.submit(() -> {
                        var client = MongoClients.create(MongoClientSettings.builder(baseClientSettings())
                                .applyConnectionString(new ConnectionString(config.uri()))
                                .build());
                        try {
                            // Ping client to ensure it is alive
                            client.listDatabaseNames().first(); // Pings database
                            Holder.logger.log(System.Logger.Level.INFO, "Connected to MongoDB cluster: {}", client.getClusterDescription());
                        } catch (Exception e) {
                            client.close();
                            throw new RuntimeException(e);
                        }
                        return client;
                    }, ForkJoinPool.commonPool()));
        }
        return Holder.instance;
    }

    /** Returns the required base client settings for the currently registered codecs. */
    static @NotNull MongoClientSettings baseClientSettings() {
        class Holder {
            static MongoClientSettings instance = null;
        }
        if (Holder.instance == null) {
            List<Codec<?>> codecs = new ArrayList<>();
            for (Codec<?> codec : ServiceLoader.load(Codec.class)) {
                codecs.add(codec);
            }
            Holder.instance = MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .codecRegistry(CodecRegistries.fromRegistries(
                            CodecRegistries.fromCodecs(codecs),
                            MongoClientSettings.getDefaultCodecRegistry()
                    ))
                    .build();
        }
        return Holder.instance;
    }

}
