package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface BanStorage {

    static @NotNull BanStorage memory() {
        return new BanStorageMemory();
    }

    static @NotNull ListenableFuture<@NotNull BanStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return Futures.transform(
                clientFactory.newClient(config),
                client -> new BanStorageMongo(client, config),
                Runnable::run
        );
    }

    @NotNull
    Future<Boolean> isPlayerBanned(@NotNull Player player);

    @NotNull
    Future<Void> banPlayer(@NotNull Player player);

    @NotNull
    Future<Void> unbanPlayer(@NotNull Player player);
}
