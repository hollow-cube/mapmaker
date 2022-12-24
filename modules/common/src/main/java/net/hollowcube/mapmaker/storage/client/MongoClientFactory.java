package net.hollowcube.mapmaker.storage.client;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public interface MongoClientFactory {

    /** Creates a new MongoClient from the given config. */
    @NotNull FutureResult<@NotNull MongoClient> newClient(@NotNull MongoConfig config);


    /** Returns the first registered factory, or the default factory if none are present. */
    static @NotNull MongoClientFactory get() {
        class Holder {
            static final Logger logger = LoggerFactory.getLogger(MongoClientFactory.class);
            static MongoClientFactory instance = null;
        }
        if (Holder.instance == null) {
            Holder.instance = ServiceLoader.load(MongoClientFactory.class).findFirst()
                    .orElseGet(() -> config -> FutureResult.supply(() -> {
                        var client = MongoClients.create(MongoClientSettings.builder(baseClientSettings())
                                .applyConnectionString(new ConnectionString(config.uri()))
                                .build());
                        try {
                            // Ping client to ensure it is alive
                            var cluster = client.getClusterDescription();
                            Holder.logger.info("Connected to MongoDB cluster: {}", cluster);
                        } catch (Exception e) {
                            client.close();
                            return Result.error(Error.of(e));
                        }
                        return Result.of(client);
                    }));
        }
        return Holder.instance;
    }

    /** Returns the required base client settings for the currently registered codecs. */
    static @NotNull MongoClientSettings baseClientSettings() {
        class Holder {
            static MongoClientSettings instance = null;
        }
        if (Holder.instance == null) {
            var codecs = ServiceLoader.load(Codec.class).stream().map(ServiceLoader.Provider::get).toArray();
            Holder.instance = MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .codecRegistry(CodecRegistries.fromRegistries(
                            CodecRegistries.fromCodecs((Codec<?>[]) codecs),
                            MongoClientSettings.getDefaultCodecRegistry()
                    ))
                    .build();
        }
        return Holder.instance;
    }

}
