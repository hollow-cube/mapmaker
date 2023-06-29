package net.hollowcube.mapmaker.map;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapSettings {

    private String name;
    private Material icon;

    private MapVariant variant;

    private Pos spawnPoint;

    public MapSettings() {
        this.name = "";
        this.icon = null;
        this.variant = MapVariant.BUILDING;
        this.spawnPoint = new Pos(0.5, 40, 0.5);
    }

    public MapSettings(
            @Nullable String name,
            @Nullable Material icon,
            @Nullable MapVariant variant,
            @Nullable Pos spawnPoint
    ) {
        this.name = name;
        this.icon = icon;
        this.variant = variant;
        this.spawnPoint = spawnPoint;
    }

    public @NotNull String getName() {
        return name;
    }
    public @NotNull Component getNameComponent() {
        if (name == null || name.isEmpty())
            return Component.text(MapData.DEFAULT_NAME);
        return Component.text(name);
    }
    public void setName(@NotNull String name) {
        this.name = name;
    }
    public @Nullable Material getIcon() {
        return icon;
    }
    public void setIcon(@NotNull Material icon) {
        this.icon = icon;
    }

    public @NotNull MapVariant getVariant() {
        return variant;
    }
    public void setVariant(@NotNull MapVariant type) {
        this.variant = type;
    }

    public @NotNull Pos getSpawnPoint() {
        return spawnPoint;
    }
    public void setSpawnPoint(@NotNull Pos spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

}
