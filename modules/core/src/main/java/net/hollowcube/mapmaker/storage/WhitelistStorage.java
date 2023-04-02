package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Future;

public interface WhitelistStorage {


    static @NotNull WhitelistStorage memory() {
        return new WhitelistStorageMemory();
    }

    static @NotNull ListenableFuture<@NotNull WhitelistStorage> mongo(@NotNull MongoConfig config) {
        var clientFactory = MongoClientFactory.get();
        return Futures.transform(
                clientFactory.newClient(config),
                client -> new WhitelistStorageMongo(client, config),
                Runnable::run
        );
    }

    default @NotNull Future<Boolean> isPlayerWhitelisted(@NotNull Player player) { return isUUIDWhitelisted(player.getUuid()); }

    @NotNull
    Future<Boolean> isUUIDWhitelisted(@NotNull UUID uuid);

    default @NotNull Future<Void> addToWhitelist(@NotNull Player player) { return addToWhitelist(player.getUuid()); }

    @NotNull
    Future<Void> addToWhitelist(@NotNull UUID uuid);

    default @NotNull Future<Void> removeFromWhitelist(@NotNull Player player) { return removeFromWhitelist(player.getUuid()); }

    @NotNull
    Future<Void> removeFromWhitelist(@NotNull UUID uuid);
}
