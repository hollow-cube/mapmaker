package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@RuntimeGson
public class MapPlayerData {
    public static final Tag<MapPlayerData> TAG = Tag.Transient("mapmaker:map_player_data");

    public static @NotNull MapPlayerData fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private String id;
    private String[] mapSlots = new String[5];
    private String contestSlot;
    private String lastPlayedMap;
    private String lastEditedMap;

    public MapPlayerData() {
    }

    public MapPlayerData(@NotNull String id) {
        this.id = id;
    }

    public MapPlayerData(
        @NotNull String id, String[] mapSlots, @Nullable String contestSlot, @Nullable String lastPlayedMap,
        @Nullable String lastEditedMap
    ) {
        this.id = id;
        this.mapSlots = mapSlots;
        this.contestSlot = contestSlot;
        this.lastPlayedMap = lastPlayedMap;
        this.lastEditedMap = lastEditedMap;
    }

    public @NotNull String id() {
        return id;
    }

    public String[] mapSlots(PlayerData playerData) {
        int unlockedSlots = playerData.mapSlots();
        if (mapSlots == null)
            mapSlots = new String[unlockedSlots];
        if (mapSlots.length < unlockedSlots)
            mapSlots = Arrays.copyOf(mapSlots, unlockedSlots);
        return mapSlots;
    }

    public @Nullable String lastPlayedMap() {
        return lastPlayedMap == null || lastPlayedMap.isEmpty() ? null : lastPlayedMap;
    }

    public @Nullable String lastEditedMap() {
        return lastEditedMap == null || lastEditedMap.isEmpty() ? null : lastEditedMap;
    }

    public void update(@NotNull MapPlayerData other) {
        this.mapSlots = other.mapSlots;
        this.lastPlayedMap = other.lastPlayedMap;
        this.lastEditedMap = other.lastEditedMap;
        this.contestSlot = other.contestSlot;
    }

    public @NotNull SlotState getSlotState(PlayerData playerData, int slot) {
        if (slot < 0 || slot >= playerData.mapSlots())
            return SlotState.LOCKED;
        if (slot >= mapSlots.length || mapSlots[slot] == null || mapSlots[slot].isEmpty())
            return SlotState.EMPTY;
        return SlotState.FILLED;
    }

    public @Nullable String getMapSlot(PlayerData playerData, int slot) {
        if (slot < 0 || slot >= playerData.mapSlots() || slot >= mapSlots.length)
            return null;
        var mapId = mapSlots[slot];
        return mapId == null || mapId.isEmpty() ? null : mapId;
    }

    public @Nullable String getContestSlot() {
        return contestSlot == null || contestSlot.isEmpty() ? null : contestSlot;
    }

    public void setContestSlot(@Nullable String contestSlot) {
        this.contestSlot = contestSlot;
    }

    @Override
    public String toString() {
        return "MapPlayerData[" +
               "id='" + id + '\'' +
               ", mapSlots=" + Arrays.toString(mapSlots) +
               ", lastPlayedMap='" + lastPlayedMap + '\'' +
               ", lastEditedMap='" + lastEditedMap + '\'' +
               ']';
    }
}
