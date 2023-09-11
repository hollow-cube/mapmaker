package net.hollowcube.mapmaker.map;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class MapSettings {

    transient MapUpdateRequest updates = new MapUpdateRequest();
    transient ReentrantLock updateLock = new ReentrantLock();

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

    /**
     * Run a callback with the update request, and if the callback returns true, reset the update request.
     */
    public void withUpdateRequest(@NotNull Function<@NotNull MapUpdateRequest, Boolean> callback) {
        updateLock.lock();
        try {
            if (updates.hasChanges() && callback.apply(updates)) {
                this.updates = new MapUpdateRequest();
            }
        } finally {
            updateLock.unlock();
        }
    }

    public @NotNull String getName() {
        return name;
    }
    public @NotNull Component getNameComponent() {
        if (name == null || name.isEmpty())
            return Component.text(MapData.DEFAULT_NAME);
        return Component.text(name);
    }
    public @NotNull Component getTagsComponent() {
        var tags = getTags();
        var maxTags = Math.min(2, tags.size());
        var currTagIdx = 0;
        var tagsLabel = Component.text("");
        // TODO find a way to terminate after a certain length of displayed text
        while (currTagIdx < maxTags) {
//            var tagName = tags.get(currTagIdx).displayName(); // TODO figure out how to get translation key working
            var tagName = Component.text(tags.get(currTagIdx).name());
            tagsLabel = tagsLabel.append(tagName);
            tagsLabel = tagsLabel.append(Component.text(", "));
            currTagIdx++;
        }
        if (currTagIdx == 0)
            tagsLabel = Component.text("None");
        if (currTagIdx < tags.size())
            tagsLabel = tagsLabel.append(Component.text(String.format("+%s", tags.size() - currTagIdx)));
        return tagsLabel;
    }
    public void setName(@NotNull String name) {
        updateLock.lock();
        try {
            updates.setName(name);
            this.name = name;
        } finally {
            updateLock.unlock();
        }
    }
    public @Nullable Material getIcon() {
        return icon;
    }
    public void setIcon(@NotNull Material icon) {
        updateLock.lock();
        try {
            updates.setIcon(icon.name());
            this.icon = icon;
        } finally {
            updateLock.unlock();
        }
    }

    public @Nullable MapSize getSize() {
        return size;
    }

    public @NotNull MapVariant getVariant() {
        return variant;
    }
    public void setVariant(@NotNull MapVariant type) {
        updateLock.lock();
        try {
            updates.setVariant(type);
            this.variant = type;
        } finally {
            updateLock.unlock();
        }
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
        updateLock.lock();
        try {
            Check.argCondition(variant != MapVariant.PARKOUR, "Parkour subvariant can only be set for parkour maps");
            this.subvariant = subvariant == null ? null : subvariant.name().toLowerCase();
            updates.setSubVariant(this.subvariant);
        } finally {
            updateLock.unlock();
        }
    }

    public @NotNull Pos getSpawnPoint() {
        return spawnPoint;
    }
    public void setSpawnPoint(@NotNull Pos spawnPoint) {
        updateLock.lock();
        try {
            updates.setSpawnPoint(spawnPoint);
            this.spawnPoint = spawnPoint;
        } finally {
            updateLock.unlock();
        }
    }

    public boolean isOnlySprint() {
        return onlySprint;
    }
    public void setOnlySprint(boolean onlySprint) {
        updateLock.lock();
        try {
            updates.setOnlySprint(onlySprint);
            this.onlySprint = onlySprint;
        } finally {
            updateLock.unlock();
        }
    }
    public boolean isNoSprint() {
        return noSprint;
    }
    public void setNoSprint(boolean noSprint) {
        updateLock.lock();
        try {
            updates.setNoSprint(noSprint);
            this.noSprint = noSprint;
        } finally {
            updateLock.unlock();
        }
    }
    public boolean isNoJump() {
        return noJump;
    }
    public void setNoJump(boolean noJump) {
        updateLock.lock();
        try {
            updates.setNoJump(noJump);
            this.noJump = noJump;
        } finally {
            updateLock.unlock();
        }
    }
    public boolean isNoSneak() {
        return noSneak;
    }
    public void setNoSneak(boolean noSneak) {
        updateLock.lock();
        try {
            updates.setNoSneak(noSneak);
            this.noSneak = noSneak;
        } finally {
            updateLock.unlock();
        }
    }
    public boolean isBoat() {
        return boat;
    }
    public void setBoat(boolean boat) {
        updateLock.lock();
        try {
            updates.setBoat(boat);
            this.boat = boat;
        } finally {
            updateLock.unlock();
        }
    }

    public List<MapTags.Tag> getTags() {
        if (this.tags == null)
            return new ArrayList<>();
        return this.tags;
    }

    public void addTag(@NotNull MapTags.Tag tag) {
        updateLock.lock();
        try {
            if (variant == MapVariant.BUILDING && tag.type == MapTags.TagType.GAMEPLAY) {
                System.out.println("you shouldn't be here! make sure you're not allowing build maps to use gameplay tags.");
            }
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            this.tags.add(tag);
            updates.setTags(this.tags);
        } finally {
            updateLock.unlock();
        }
    }

    public void removeTag(@NotNull MapTags.Tag tag) {
        updateLock.lock();
        try {
            if (this.tags != null) {
                this.tags.remove(tag);
                updates.setTags(this.tags);
            }
        } finally {
            updateLock.unlock();
        }
    }
}
