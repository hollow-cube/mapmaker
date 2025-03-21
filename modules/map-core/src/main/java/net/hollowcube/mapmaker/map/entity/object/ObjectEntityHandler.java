package net.hollowcube.mapmaker.map.entity.object;

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

    public void onDataChange(@Nullable Player player) {
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

    public void addViewer(@NotNull Player player) {

    }

    public void removeViewer(@NotNull Player player) {

    }

}
