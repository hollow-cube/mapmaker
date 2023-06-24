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

public class SaveStateV2 {
    public static final Tag<SaveStateV2> TAG = Tag.Transient("mapmaker:map/save_state");

    public static @NotNull SaveStateV2 fromPlayer(@NotNull Player player) {
        return Objects.requireNonNull(optionalFromPlayer(player));
    }

    public static @Nullable SaveStateV2 optionalFromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private String id;
    private String playerId;
    private String mapId;
    private SaveStateType type;
    private Instant created;
    private Instant lastModified;
    private boolean completed;
    private long playtime;

    // Common to play and edit
    private Pos pos = null;

    // Editing
    private String inventory = null; // base64 bytes
    private String tfstate = null; // base64 bytes

    // Playing
    private String checkpoint = null;

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
        this.completed = completed;
    }

    public long getPlaytime() {
        return playtime;
    }
    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }

    public @Nullable Pos pos() {
        return pos;
    }

    public byte @Nullable [] inventory() {
        if (inventory == null) return null;
        return Base64.getDecoder().decode(inventory);
    }
    public byte @Nullable [] tfState() {
        if (tfstate == null) return null;
        return Base64.getDecoder().decode(tfstate);
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

}
