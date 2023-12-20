package net.hollowcube.mapmaker.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTList;
import org.jglrxavpok.hephaistos.nbt.NBTType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;

public class SaveState {
    private static final Logger logger = LoggerFactory.getLogger(SaveState.class);

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
    private boolean isFlying = false;
    private String inventory = null; // base64 bytes

    // Playing
    private String checkpoint = null;
    private Pos checkpointPos = null;

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

        // If completing, update the playtime for the final time.
        if (completed) {
            updatePlaytime();
            playStartTime = 0;
        }
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

    public boolean isFlying() {
        return isFlying;
    }

    public void setFlying(boolean flying) {
        updates.setFlying(flying);
        isFlying = flying;
    }

    public byte @Nullable [] inventory() {
        if (inventory == null) return null;
        return Base64.getDecoder().decode(inventory);
    }

    public @Nullable String checkpoint() {
        return checkpoint;
    }

    public void setCheckpoint(@Nullable String checkpoint, @NotNull Pos pos) {
        this.checkpoint = checkpoint;
        this.checkpointPos = pos;
        updates.setCheckpoint(checkpoint == null ? "" : checkpoint);
        updates.setCheckpointPos(pos);
    }

    public @Nullable Pos checkpointPos() {
        return checkpointPos;
    }

    // Utilities

    @SuppressWarnings("UnstableApiUsage")
    public @Nullable List<@NotNull ItemStack> getInventoryItems() {
        var inventory = inventory();
        if (inventory == null) return null;
        try {
            var compound = (NBTCompound) new NetworkBuffer(ByteBuffer.wrap(inventory)).read(NetworkBuffer.NBT);
            var items = compound.<NBTCompound>getList("Items");
            if (items == null) return null;
            return items.asListView().stream().map(ItemStack::fromItemNBT).toList();
        } catch (Exception e) {
            try {
                logger.warn("Failed to read modern inventory, trying legacy: {}", e.getMessage());
                return new NetworkBuffer(ByteBuffer.wrap(inventory))
                        .readCollection(NetworkBuffer.ITEM);
            } catch (Exception e2) {
                MinecraftServer.getExceptionManager().handleException(e2);
                return null;
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void setInventoryItems(@Nullable List<ItemStack> items) {
        if (items == null) {
            this.inventory = null;
        } else {
            var entries = new ArrayList<NBTCompound>();
            for (var item : items) entries.add(item.toItemNBT());
            var inventoryTag = new NBTCompound(Map.of(
                    "Items", new NBTList<>(NBTType.TAG_Compound, entries)
                    // Space left for other tags
            ));
            this.inventory = Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(b -> b.write(NetworkBuffer.NBT, inventoryTag)));
        }
        updates.setInventory(this.inventory);
    }

}
