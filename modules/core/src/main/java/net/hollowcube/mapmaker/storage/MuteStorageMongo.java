package net.hollowcube.mapmaker.storage;

import com.mongodb.client.MongoClient;
import net.hollowcube.common.config.MongoConfig;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public class MuteStorageMongo extends BasicUUIDStorageMongo implements MuteStorage {

    public MuteStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        super(client, config, "mutes");
    }

    @Override
    public @NotNull Future<Boolean> isPlayerMuted(@NotNull Player player) {
        return isPlayerStored(player);
    }

    @Override
    public @NotNull Future<Void> mutePlayer(@NotNull Player player) {
        return addPlayerToStore(player);
    }

    @Override
    public @NotNull Future<Void> ummutePlayer(@NotNull Player player) {
        return removePlayerFromStore(player);
    }
}
