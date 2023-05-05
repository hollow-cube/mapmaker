package net.hollowcube.mapmaker.permission;

import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

public class MapPermissionManagerNoop implements MapPermissionManager {
    @Override
    public @NotNull String addMapOwner(@NotNull String mapId, @NotNull String playerId) {
        return "";
    }

    @Override
    public @NotNull String makeMapPublic(@NotNull String mapId) {
        return "";
    }

    @Override
    public @NotNull String deleteMap(@NotNull String mapId) {
        return "";
    }

    @Override
    public boolean checkPermission(@NotNull String mapId, @NotNull String playerId, MapData.@NotNull Permission permission) {
        return true;
    }
}
