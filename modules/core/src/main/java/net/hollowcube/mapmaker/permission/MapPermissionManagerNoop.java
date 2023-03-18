package net.hollowcube.mapmaker.permission;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

public class MapPermissionManagerNoop implements MapPermissionManager {
    @Override
    public @NotNull ListenableFuture<@NotNull String> addMapOwner(@NotNull String mapId, @NotNull String playerId) {
        return Futures.immediateFuture("");
    }

    @Override
    public @NotNull ListenableFuture<@NotNull String> makeMapPublic(@NotNull String mapId) {
        return Futures.immediateFuture("");
    }

    @Override
    public @NotNull ListenableFuture<String> deleteMap(@NotNull String mapId) {
        return Futures.immediateFuture("");
    }

    @Override
    public @NotNull ListenableFuture<Boolean> checkPermission(@NotNull String mapId, @NotNull String playerId, MapData.@NotNull Permission permission) {
        return Futures.immediateFuture(true);
    }
}
