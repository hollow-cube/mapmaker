package net.hollowcube.mapmaker.map.entity.marker;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MarkerHandler {
    private final String id;
    protected final MarkerEntity entity;

    protected MarkerHandler(@NotNull String id, @NotNull MarkerEntity entity) {
        this.id = id;
        this.entity = entity;
    }

    public @NotNull String id() {
        return id;
    }

    protected void onDataChange(@Nullable Player player) {
    }

    protected void onRemove() {
    }

    protected void onTick() {
    }

    protected void onPlayerEnter(@NotNull Player player) {
    }

    protected void onPlayerExit(@NotNull Player player) {
    }

    protected void addViewer(@NotNull Player player) {

    }

    protected void removeViewer(@NotNull Player player) {

    }

}
