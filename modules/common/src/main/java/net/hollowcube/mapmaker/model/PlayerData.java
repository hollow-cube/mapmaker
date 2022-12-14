package net.hollowcube.mapmaker.model;

import net.hollowcube.mapmaker.util.TagUtil;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * MapMaker data for a single player.
 */
public class PlayerData {
    public static final Tag<String> PLAYER_ID = Tag.String("mapmaker:player_id");

    public static final Tag<PlayerData> DATA = TagUtil.noop("mapmaker:player_data");

    public static final int MAX_MAP_SLOTS = 5;

    private String id;
    private String uuid;

    private int unlockedMapSlots;
    private String[] mapSlots = new String[MAX_MAP_SLOTS];

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getUnlockedMapSlots() {
        return unlockedMapSlots;
    }

    public void setUnlockedMapSlots(int unlockedMapSlots) {
        this.unlockedMapSlots = unlockedMapSlots;
    }

    public void setMapSlots(String[] mapSlots) {
        this.mapSlots = mapSlots;
    }

    public @NotNull List<String> getMapSlots() {
        return List.of(mapSlots);
    }

    public @Nullable String getMapSlot(int slot) {
        Check.argCondition(slot < 0 || slot >= MAX_MAP_SLOTS, "Slot must be between 0 and " + MAX_MAP_SLOTS);
        return mapSlots[slot];
    }

    public void setMapSlot(int slot, @Nullable String mapId) {
        Check.argCondition(slot < 0 || slot >= MAX_MAP_SLOTS, "Slot must be between 0 and " + MAX_MAP_SLOTS);
        mapSlots[slot] = mapId;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "id='" + id + '\'' +
                '}';
    }
}
