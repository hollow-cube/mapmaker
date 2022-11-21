package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface PlayerStorage extends Storage {

    static @NotNull PlayerStorage memory() {
        return new PlayerStorageMemory();
    }

    @NotNull CompletableFuture<@NotNull PlayerData> createPlayer(@NotNull PlayerData player);

    @NotNull CompletableFuture<@NotNull PlayerData> getPlayerById(@NotNull String id);

    @NotNull CompletableFuture<@NotNull PlayerData> getPlayerByUuid(@NotNull String uuid);

}
