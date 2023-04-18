package net.hollowcube.mapmaker.storage;

import com.mongodb.client.MongoClient;
import net.hollowcube.common.config.MongoConfig;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public class BanStorageMongo extends BasicUUIDStorageMongo implements BanStorage {


    public BanStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        super(client, config, "bans");
    }

    @Override
    public @NotNull Future<Boolean> isPlayerBanned(@NotNull Player player) {
        return isPlayerStored(player);
    }

    @Override
    public @NotNull Future<Void> banPlayer(@NotNull Player player) {
        return addPlayerToStore(player);
    }

    @Override
    public @NotNull Future<Void> unbanPlayer(@NotNull Player player) {
        return removePlayerFromStore(player);
    }
}
