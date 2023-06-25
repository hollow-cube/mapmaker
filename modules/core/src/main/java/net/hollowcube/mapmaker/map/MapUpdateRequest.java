package net.hollowcube.mapmaker.map;

import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapUpdateRequest {
    private String name = null;
    private String icon = null;
    private MapVariant variant = null;
    private Pos spawnPoint = null;

    public boolean hasChanges() {
        return name != null || icon != null || variant != null || spawnPoint != null;
    }

    public @Nullable String getName() {
        return name;
    }
    public @NotNull MapUpdateRequest setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    public @Nullable String getIcon() {
        return icon;
    }
    public @NotNull MapUpdateRequest setIcon(@Nullable String icon) {
        this.icon = icon;
        return this;
    }

    public @Nullable MapVariant getVariant() {
        return variant;
    }
    public void setVariant(@Nullable MapVariant variant) {
        this.variant = variant;
    }

    public @Nullable Pos getSpawnPoint() {
        return spawnPoint;
    }
    public void setSpawnPoint(@Nullable Pos spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

}
