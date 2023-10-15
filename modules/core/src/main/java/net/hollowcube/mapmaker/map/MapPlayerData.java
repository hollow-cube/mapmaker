package net.hollowcube.mapmaker.map;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class MapPlayerData {
    public static final Tag<MapPlayerData> TAG = Tag.Transient("mapmaker:map_player_data");

    public static @NotNull MapPlayerData fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private String id;
    private int unlockedSlots = 5;
    private String[] mapSlots = new String[5];
    private String lastPlayedMap;
    private String lastEditedMap;

    public MapPlayerData() {
    }

    public MapPlayerData(@NotNull String id) {
        this.id = id;
    }

    public MapPlayerData(@NotNull String id, int unlockedSlots, String[] mapSlots, @Nullable String lastPlayedMap, @Nullable String lastEditedMap) {
        this.id = id;
        this.unlockedSlots = unlockedSlots;
        this.mapSlots = mapSlots;
        this.lastPlayedMap = lastPlayedMap;
        this.lastEditedMap = lastEditedMap;
    }

    public @NotNull String id() {
        return id;
    }

    public int unlockedSlots() {
        return unlockedSlots;
    }

    public String[] mapSlots() {
        return mapSlots;
    }

    public @Nullable String lastPlayedMap() {
        return lastPlayedMap == null || lastPlayedMap.isEmpty() ? null : lastPlayedMap;
    }

    public @Nullable String lastEditedMap() {
        return lastEditedMap == null || lastEditedMap.isEmpty() ? null : lastEditedMap;
    }

    public void update(@NotNull MapPlayerData other) {
        this.unlockedSlots = other.unlockedSlots;
        this.mapSlots = other.mapSlots;
        this.lastPlayedMap = other.lastPlayedMap;
        this.lastEditedMap = other.lastEditedMap;
    }

    public @NotNull SlotState getSlotState(int slot) {
        if (slot < 0 || slot >= unlockedSlots)
            return SlotState.LOCKED;
        if (slot >= mapSlots.length || mapSlots[slot] == null || mapSlots[slot].isEmpty())
            return SlotState.EMPTY;
        return SlotState.FILLED;
    }

    public @Nullable String getMapSlot(int slot) {
        if (slot < 0 || slot >= unlockedSlots || slot >= mapSlots.length)
            return null;
        var mapId = mapSlots[slot];
        return mapId == null || mapId.isEmpty() ? null : mapId;
    }

    @Override
    public String toString() {
        return "MapPlayerData[" +
                "id='" + id + '\'' +
                ", unlockedSlots=" + unlockedSlots +
                ", mapSlots=" + Arrays.toString(mapSlots) +
                ", lastPlayedMap='" + lastPlayedMap + '\'' +
                ", lastEditedMap='" + lastEditedMap + '\'' +
                ']';
    }
}
