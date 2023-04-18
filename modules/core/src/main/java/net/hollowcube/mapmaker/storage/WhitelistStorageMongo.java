package net.hollowcube.mapmaker.storage;

import com.mongodb.client.MongoClient;
import net.hollowcube.common.config.MongoConfig;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Future;

public class WhitelistStorageMongo extends BasicUUIDStorageMongo implements WhitelistStorage {

    public WhitelistStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        super(client, config, "whitelist");
    }

    @Override
    public @NotNull Future<Boolean> isUUIDWhitelisted(@NotNull UUID uuid) {
        return isUUIDStored(uuid);
    }

    @Override
    public @NotNull Future<Void> addToWhitelist(@NotNull UUID uuid) {
        return addUUIDToStore(uuid);
    }

    @Override
    public @NotNull Future<Void> removeFromWhitelist(@NotNull UUID uuid) {
        return removeUUIDFromStore(uuid);
    }
}
