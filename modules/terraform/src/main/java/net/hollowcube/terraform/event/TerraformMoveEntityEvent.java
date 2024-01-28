package net.hollowcube.terraform.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TerraformMoveEntityEvent implements EntityInstanceEvent, CancellableEvent {

    private final Player source;
    private final Entity entity;
    private Pos newPosition;

    private boolean cancelled = false;

    public TerraformMoveEntityEvent(@Nullable Player source, @NotNull Entity entity, @NotNull Pos newPosition) {
        this.source = source;
        this.entity = entity;
        this.newPosition = newPosition;
    }

    public @Nullable Player getSource() {
        return source;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    public @NotNull Pos getOldPosition() {
        return entity.getPosition();
    }

    public @NotNull Pos getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(@NotNull Pos newPosition) {
        this.newPosition = newPosition;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
