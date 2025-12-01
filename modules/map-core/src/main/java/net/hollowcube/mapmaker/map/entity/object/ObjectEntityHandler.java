package net.hollowcube.mapmaker.map.entity.object;

import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ObjectEntityHandler {
    private final String id;
    protected final ObjectEntity entity;

    protected ObjectEntityHandler(@NotNull String id, @NotNull ObjectEntity entity) {
        this.id = id;
        this.entity = entity;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull CompoundBinaryTag data() {
        return entity.getData();
    }

    public void onDataChange(@Nullable Player player) {
    }

    public void onPositionChange(@NotNull Pos newPosition) {
    }

    public void onRemove() {
    }

    public void onTick() {
    }

    public void onPlayerInteract(@NotNull Player player) {
    }

    public void onPlayerEnter(@NotNull Player player) {
    }

    public void onPlayerExit(@NotNull Player player) {
    }

    public boolean canSendToPlayer(@NotNull Player player) {
        return true;
    }

    public void addViewer(@NotNull MapWorld world, @NotNull Player player) {

    }

    public void removeViewer(@NotNull MapWorld world, @NotNull Player player) {

    }

}
