package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.map.SaveStateUpdateRequest;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Base64;

public class PlayerDataV2 {
    public static final Tag<PlayerDataV2> TAG = Tag.Transient("mapmaker:player_data");
    public static @NotNull PlayerDataV2 fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    public static final int MAX_MAP_SLOTS = 5;

    private transient PlayerDataUpdateRequest updates = new PlayerDataUpdateRequest();

    private String id;
    private String username;
    private Component displayName;

    private int unlockedMapSlots = 2;
    private String[] mapSlots = new String[MAX_MAP_SLOTS];

    private String tfState = null; // base64 bytes

    public PlayerDataV2() {
    }

    public PlayerDataV2(@NotNull String id, @NotNull String username, @NotNull Component displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }

    public @NotNull PlayerDataUpdateRequest getUpdateRequest() {
        var updates = this.updates;
        this.updates = new PlayerDataUpdateRequest();
        return updates;
    }

    public @NotNull String id() {
        return id;
    }
    public @NotNull String username() {
        return username;
    }
    public @NotNull Component displayName() {
        return displayName;
    }

    public int getUnlockedMapSlots() {
        return unlockedMapSlots;
    }

    public void setUnlockedMapSlots(int unlockedMapSlots) {
        updates.setUnlockedMapSlots(unlockedMapSlots);
        this.unlockedMapSlots = unlockedMapSlots;
    }

    @ApiStatus.Internal
    public String[] getRawMapSlots() {
        return mapSlots;
    }

    public @NotNull SlotState getSlotState(int slot) {
        if (slot < 0 || slot >= unlockedMapSlots)
            return SlotState.LOCKED;
        if (slot >= mapSlots.length || mapSlots[slot] == null)
            return SlotState.EMPTY;
        return SlotState.FILLED;
    }

    public @Nullable String getMapSlot(int slot) {
        if (slot < 0 || slot >= unlockedMapSlots || slot >= mapSlots.length)
            return null;
        return mapSlots[slot];
    }

    public boolean setMapSlot(int slot, @Nullable String mapId) {
        if (slot < 0 || slot >= unlockedMapSlots)
            return false;

        // Resize if necessary, then set
        if (slot >= mapSlots.length)
            mapSlots = Arrays.copyOf(mapSlots, slot + 1);
        mapSlots[slot] = mapId;
        updates.setMapSlots(mapSlots);
        return true;
    }

    public byte @Nullable [] getTfState() {
        if (tfState == null) return null;
        return Base64.getDecoder().decode(tfState);
    }

    public void setTfState(byte @Nullable [] tfState) {
        if (tfState == null) this.tfState = null;
        else this.tfState = Base64.getEncoder().encodeToString(tfState);
        updates.setTfState(this.tfState);
    }
}
