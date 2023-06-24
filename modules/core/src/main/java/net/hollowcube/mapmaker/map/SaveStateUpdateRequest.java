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
    private String inventory = null;
    private String tfstate = null;
    private String checkpoint = null;

    public boolean hasChanges() {
        return completed != null || playtime != null || pos != null || inventory != null || tfstate != null || checkpoint != null;
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
        this.tfstate = Base64.getEncoder().encodeToString(tfstate);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setCheckpoint(@NotNull String checkpoint) {
        this.checkpoint = checkpoint;
        return this;
    }

}
