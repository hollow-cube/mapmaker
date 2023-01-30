package net.hollowcube.mapmaker.model;

import net.hollowcube.common.util.ExtraTags;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A player may have one or more save state for each map they have played.
 */
public class SaveState {
    public static final Tag<SaveState> TAG = ExtraTags.Transient("mapmaker:map/save_state");

    public static @NotNull SaveState fromPlayer(@NotNull Player player) {
        return Objects.requireNonNull(player.getTag(TAG));
    }

    private String id;
    private String playerId;
    private String mapId;
    private boolean editing = false;
    private boolean completed = false;

    // The time this save state was created.
    private Instant startTime;
    // The total time spent inside this save state.
    private long playtime;
    private transient long playtimeUpdate;

    private Pos pos;                    // Used when `save_exact_pos=on`
    private String checkpoint;          // Used when a checkpoint is present
    private List<ItemStack> inventory;  // Used in build mode & when `save_inventory=on`
    private NBTCompound nbt;            // Present for editing savestates, otherwise not.

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }

    public long getPlaytimeUpdate() {
        return playtimeUpdate;
    }

    public void setPlaytimeUpdate(long playtimeUpdate) {
        this.playtimeUpdate = playtimeUpdate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Pos getPos() {
        return pos;
    }

    public void setPos(Pos pos) {
        this.pos = pos;
    }

    public String getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(String checkpoint) {
        this.checkpoint = checkpoint;
    }

    public List<ItemStack> getInventory() {
        if (this.inventory == null)
            return List.of();
        return inventory;
    }

    public void setInventory(List<ItemStack> inventory) {
        this.inventory = inventory;
    }

    public @Nullable NBTCompound getNbt() {
        return nbt;
    }

    public void setNbt(@Nullable NBTCompound nbt) {
        this.nbt = nbt;
    }
}
