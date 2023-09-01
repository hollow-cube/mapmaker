package net.hollowcube.mapmaker.map;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MapSettings {

    private transient MapUpdateRequest updates = new MapUpdateRequest();

    private String name;
    private Material icon;
    private MapSize size;
    private MapVariant variant;
    private String subvariant;

    private Pos spawnPoint;

    // Gameplay settings
    private boolean onlySprint = false;
    private boolean noSprint = false;
    private boolean noJump = false;
    private boolean noSneak = false;
    private boolean boat = false;

    private List<MapTags.Tag> tags;

    public MapSettings() {
        this.name = "";
        this.icon = null;
        this.variant = MapVariant.BUILDING;
        this.size = MapSize.NORMAL;
        this.spawnPoint = new Pos(0.5, 40, 0.5);
        this.tags = new ArrayList<>();
    }

    public MapSettings(
            @Nullable String name,
            @Nullable Material icon,
            @Nullable MapSize size,
            @Nullable MapVariant variant,
            @Nullable Pos spawnPoint,
            boolean onlySprint,
            boolean noSprint,
            boolean noJump,
            boolean noSneak,
            @Nullable List<MapTags.Tag> tags
    ) {
        this.name = name;
        this.icon = icon;
        this.size = size;
        this.variant = variant;
        this.spawnPoint = spawnPoint;
        this.onlySprint = onlySprint;
        this.noSprint = noSprint;
        this.noJump = noJump;
        this.noSneak = noSneak;
        this.tags = tags;
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

    public @Nullable MapSize getSize() {
        return size;
    }

    public void setSize(@NotNull MapSize mapSize) {
        this.size = mapSize;
    }

    public @NotNull MapVariant getVariant() {
        return variant;
    }
    public void setVariant(@NotNull MapVariant type) {
        updates.setVariant(type);
        this.variant = type;
    }

    public @Nullable ParkourSubVariant getParkourSubVariant() {
        if (subvariant == null) return null;
        try {
            return ParkourSubVariant.valueOf(subvariant.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setSubVariant(@Nullable ParkourSubVariant subvariant) {
        Check.argCondition(variant != MapVariant.PARKOUR, "Parkour subvariant can only be set for parkour maps");
        this.subvariant = subvariant == null ? null : subvariant.name().toLowerCase();
        updates.setSubVariant(this.subvariant);
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
    public boolean isBoat() {
        return boat;
    }
    public void setBoat(boolean boat) {
        updates.setBoat(boat);
        this.boat = boat;
    }

    public List<MapTags.Tag> getTags() {
        return this.tags;
    }

    public void addTag(@NotNull MapTags.Tag tag) {
        if (variant == MapVariant.BUILDING && tag.type == MapTags.TagType.GAMEPLAY) {
            System.out.println("you shouldn't be here! make sure you're not allowing build maps to use gameplay tags.");
        }
        updates.tags.add(tag);
        this.tags.add(tag);
    }

    public void removeTag(@NotNull MapTags.Tag tag) {
        this.tags.remove(tag);
    }
}
