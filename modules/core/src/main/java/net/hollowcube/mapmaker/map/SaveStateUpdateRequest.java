package net.hollowcube.mapmaker.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.List;

public class SaveStateUpdateRequest {
    private Boolean completed = null;
    private Long playtime = null;
    private Pos pos = null;
    private Boolean isFlying = null;
    private String inventory = null;
    private String tfState = null;
    private String checkpoint = null;
    private Pos checkpointPos = null;

    public boolean hasChanges() {
        return completed != null || playtime != null || pos != null || isFlying != null || inventory != null || tfState != null || checkpoint != null || checkpointPos != null;
    }

    public @NotNull SaveStateUpdateRequest setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public @NotNull SaveStateUpdateRequest setPlaytime(long playtime) {
        this.playtime = playtime;
        return this;
    }

    public @NotNull SaveStateUpdateRequest setPos(@NotNull Pos pos) {
        this.pos = pos;
        return this;
    }

    public @NotNull SaveStateUpdateRequest setFlying(Boolean flying) {
        isFlying = flying;
        return this;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }

    @SuppressWarnings("UnstableApiUsage")
    public @NotNull SaveStateUpdateRequest setInventoryItems(@Nullable List<ItemStack> items) {
        if (items == null) {
            this.inventory = null;
        } else {
            this.inventory = Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(b -> b.writeCollection(NetworkBuffer.ITEM, items)));
        }
        return this;
    }

    public @NotNull SaveStateUpdateRequest setTFState(@NotNull byte[] tfstate) {
        this.tfState = Base64.getEncoder().encodeToString(tfstate);
        return this;
    }

    public void setTfState(String tfState) {
        this.tfState = tfState;
    }

    public @NotNull SaveStateUpdateRequest setCheckpoint(@NotNull String checkpoint) {
        this.checkpoint = checkpoint;
        return this;
    }

    public @NotNull SaveStateUpdateRequest setCheckpointPos(@NotNull Pos checkpointPos) {
        this.checkpointPos = checkpointPos;
        return this;
    }
}
