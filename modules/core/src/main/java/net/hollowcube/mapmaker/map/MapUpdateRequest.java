package net.hollowcube.mapmaker.map;

import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapUpdateRequest {
    String name = null;
    String icon = null;
    MapVariant variant = null;
    Pos spawnPoint = null;

    Boolean onlySprint = null;
    Boolean noSprint = null;
    Boolean noJump = null;
    Boolean noSneak = null;

    public boolean hasChanges() {
        return name != null || icon != null || variant != null || spawnPoint != null ||
                onlySprint != null || noSprint != null || noJump != null || noSneak != null;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }
    public void setIcon(@Nullable String icon) {
        this.icon = icon;
    }

    public void setVariant(@Nullable MapVariant variant) {
        this.variant = variant;
    }

    public void setSpawnPoint(@Nullable Pos spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public void setOnlySprint(boolean onlySprint) {
        this.onlySprint = onlySprint;
    }
    public void setNoSprint(boolean noSprint) {
        this.noSprint = noSprint;
    }
    public void setNoJump(boolean noJump) {
        this.noJump = noJump;
    }
    public void setNoSneak(boolean noSneak) {
        this.noSneak = noSneak;
    }

}
