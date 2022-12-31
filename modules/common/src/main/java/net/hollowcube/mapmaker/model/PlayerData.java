package net.hollowcube.mapmaker.model;

import net.hollowcube.common.util.ExtraTags;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * MapMaker data for a single player.
 */
public class PlayerData {
    public static final Tag<String> PLAYER_ID = Tag.String("mapmaker:player_id");

    public static final Tag<PlayerData> DATA = ExtraTags.Transient("mapmaker:player_data");

    public static @NotNull PlayerData fromPlayer(@NotNull Player player) {
        return player.getTag(DATA);
    }

    public static final int MAX_MAP_SLOTS = 5;
    public static final int DEFAULT_UNLOCKED_MAP_SLOTS = 2;

    public static final int SLOT_STATE_OPEN = 0;
    public static final int SLOT_STATE_LOCKED = 1;
    public static final int SLOT_STATE_IN_USE = 2;

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

    public void setMapSlots(@NotNull String[] mapSlots) {
        this.mapSlots = mapSlots;
    }

    // This type definition is disgusting, but means that the array itself is never null, but entries inside it may be.
    public @Nullable String @NotNull [] getMapSlots() {
        return Arrays.copyOf(mapSlots, mapSlots.length);
    }

    public int getSlotState(int slot) {
        if (!(slot >= 0 && slot < unlockedMapSlots))
            return 1; // Map locked
        return mapSlots[slot] == null ? 0 : 2; // Map already exists
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
