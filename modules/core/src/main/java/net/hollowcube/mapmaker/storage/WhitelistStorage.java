package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface WhitelistStorage {

    static @NotNull WhitelistStorage memory() {
        return new WhitelistStorageMemory();
    }

    @Blocking
    static @NotNull WhitelistStorage mongo(@NotNull MongoConfig config) {
        var client = MongoClientFactory.get().newClient(config);
        return new WhitelistStorageMongo(client, config);
    }

    @Blocking
    boolean isWhitelisted(@NotNull String playerId);

    @Blocking
    void addToWhitelist(@NotNull String playerId);

    @Blocking
    void removeFromWhitelist(@NotNull String playerId);
}
