package net.hollowcube.mapmaker.map;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapSettings {

    private transient MapUpdateRequest updates;

    private String name;
    private Material icon;

    private MapVariant variant;

    private Pos spawnPoint;

    // Gameplay settings
    private boolean onlySprint = false;
    private boolean noSprint = false;
    private boolean noJump = false;
    private boolean noSneak = false;

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
            @Nullable Pos spawnPoint,
            boolean onlySprint,
            boolean noSprint,
            boolean noJump,
            boolean noSneak
    ) {
        this.name = name;
        this.icon = icon;
        this.variant = variant;
        this.spawnPoint = spawnPoint;
        this.onlySprint = onlySprint;
        this.noSprint = noSprint;
        this.noJump = noJump;
        this.noSneak = noSneak;
    }

    public @NotNull MapUpdateRequest getUpdateRequest() {
        var updates = this.updates;
        this.updates = new MapUpdateRequest();
        return updates;
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
        updates.setName(name);
        this.name = name;
    }
    public @Nullable Material getIcon() {
        return icon;
    }
    public void setIcon(@NotNull Material icon) {
        updates.setIcon(icon.name());
        this.icon = icon;
    }

    public @NotNull MapVariant getVariant() {
        return variant;
    }
    public void setVariant(@NotNull MapVariant type) {
        updates.setVariant(type);
        this.variant = type;
    }

    public @NotNull Pos getSpawnPoint() {
        return spawnPoint;
    }
    public void setSpawnPoint(@NotNull Pos spawnPoint) {
        updates.setSpawnPoint(spawnPoint);
        this.spawnPoint = spawnPoint;
    }

    public boolean isOnlySprint() {
        return onlySprint;
    }
    public void setOnlySprint(boolean onlySprint) {
        updates.setOnlySprint(onlySprint);
        this.onlySprint = onlySprint;
    }
    public boolean isNoSprint() {
        return noSprint;
    }
    public void setNoSprint(boolean noSprint) {
        updates.setNoSprint(noSprint);
        this.noSprint = noSprint;
    }
    public boolean isNoJump() {
        return noJump;
    }
    public void setNoJump(boolean noJump) {
        updates.setNoJump(noJump);
        this.noJump = noJump;
    }
    public boolean isNoSneak() {
        return noSneak;
    }
    public void setNoSneak(boolean noSneak) {
        updates.setNoSneak(noSneak);
        this.noSneak = noSneak;
    }

}
