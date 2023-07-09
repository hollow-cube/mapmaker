package net.hollowcube.mapmaker.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class SaveState {
    public static final Tag<SaveState> TAG = Tag.Transient("mapmaker:map/save_state");

    public static @NotNull SaveState fromPlayer(@NotNull Player player) {
        return Objects.requireNonNull(optionalFromPlayer(player));
    }

    public static @Nullable SaveState optionalFromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private transient SaveStateUpdateRequest updates = new SaveStateUpdateRequest();

    private String id;
    private String playerId;
    private String mapId;
    private SaveStateType type;
    private Instant created;
    private Instant lastModified;
    private boolean completed;
    private long playtime;
    private transient long playStartTime;

    // Common to play and edit
    private Pos pos = null;

    // Editing
    private String inventory = null; // base64 bytes
    private String tfState = null; // base64 bytes

    // Playing
    private String checkpoint = null;

    public SaveState() {
    }

    public SaveState(@NotNull String id, @NotNull String playerId, @NotNull String mapId) {
        this.id = id;
        this.playerId = playerId;
        this.mapId = mapId;
    }

    public @NotNull SaveStateUpdateRequest getUpdateRequest() {
        var updates = this.updates;
        this.updates = new SaveStateUpdateRequest();
        return updates;
    }

    public @NotNull String id() {
        return id;
    }
    public @NotNull String playerId() {
        return playerId;
    }
    public @NotNull String mapId() {
        return mapId;
    }

    public @NotNull SaveStateType type() {
        return type;
    }

    public @NotNull Instant created() {
        return created;
    }
    public @NotNull Instant lastModified() {
        return lastModified;
    }

    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        updates.setCompleted(completed);
        this.completed = completed;
    }

    public long getPlaytime() {
        return playtime;
    }
    public void setPlaytime(long playtime) {
        updates.setPlaytime(playtime);
        this.playtime = playtime;
    }
    public long getPlayStartTime() {
        return playStartTime;
    }
    public void setPlayStartTime(long playStartTime) {
        this.playStartTime = playStartTime;
    }
    public void updatePlaytime() {
        if (playStartTime == 0) return;
        setPlaytime(playtime + System.currentTimeMillis() - playStartTime);
        playStartTime = System.currentTimeMillis();
    }

    public @Nullable Pos pos() {
        return pos;
    }

    public void setPos(Pos pos) {
        updates.setPos(pos);
        this.pos = pos;
    }

    public byte @Nullable [] inventory() {
        if (inventory == null) return null;
        return Base64.getDecoder().decode(inventory);
    }
    public byte @Nullable [] tfState() {
        if (tfState == null) return null;
        return Base64.getDecoder().decode(tfState);
    }
    public void setTFState(byte @NotNull [] tfstate) {
        this.tfState = Base64.getEncoder().encodeToString(tfstate);
        updates.setTfState(this.tfState);
    }

    public @Nullable String checkpoint() {
        return checkpoint;
    }

    // Utilities

    @SuppressWarnings("UnstableApiUsage")
    public @Nullable List<@NotNull ItemStack> getInventoryItems() {
        var inventory = inventory();
        if (inventory == null) return null;
        return new NetworkBuffer(ByteBuffer.wrap(inventory))
                .readCollection(NetworkBuffer.ITEM);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void setInventoryItems(@Nullable List<ItemStack> items) {
        if (items == null) {
            this.inventory = null;
        } else {
            this.inventory = Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(b -> b.writeCollection(NetworkBuffer.ITEM, items)));
        }
        updates.setInventory(this.inventory);
    }

}
