package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface MuteStorage {

    static @NotNull MuteStorage memory() {
        return new MuteStorageMemory();
    }

    static @NotNull ListenableFuture<@NotNull MuteStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return Futures.transform(
                clientFactory.newClient(config),
                client -> new MuteStorageMongo(client, config),
                Runnable::run
        );
    }

    @NotNull
    Future<Boolean> isPlayerMuted(@NotNull Player player);

    @NotNull
    Future<Void> mutePlayer(@NotNull Player player);

    @NotNull
    Future<Void> ummutePlayer(@NotNull Player player);
}
