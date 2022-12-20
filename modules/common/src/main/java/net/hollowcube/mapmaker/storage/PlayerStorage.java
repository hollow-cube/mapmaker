package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.result.Error;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.util.MongoUtil;
import org.jetbrains.annotations.NotNull;

public interface PlayerStorage {

    Error ERR_NOT_FOUND = Error.of("player not found");
    Error ERR_DUPLICATE_ENTRY = Error.of("player already exists");

    static @NotNull PlayerStorage memory() {
        return new PlayerStorageMemory();
    }

    static @NotNull PlayerStorage mongo(@NotNull String uri) {
        return new PlayerStorageMongo(MongoUtil.getClient(uri));
    }

    @NotNull FutureResult<@NotNull PlayerData> createPlayer(@NotNull PlayerData player);

    @NotNull FutureResult<@NotNull PlayerData> getPlayerById(@NotNull String id);

    @NotNull FutureResult<@NotNull PlayerData> getPlayerByUuid(@NotNull String uuid);

    @NotNull FutureResult<@NotNull Void> updatePlayer(@NotNull PlayerData player);

}
