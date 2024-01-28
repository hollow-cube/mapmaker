package net.hollowcube.terraform.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.EntityEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TerraformSpawnEntityEvent implements InstanceEvent, EntityEvent, CancellableEvent {
    private final Player source;
    private final Instance instance;
    private Entity entity;
    private Pos position;

    private boolean cancelled = false;

    public TerraformSpawnEntityEvent(@Nullable Player source, @NotNull Instance instance, @NotNull Entity entity, @NotNull Pos position) {
        this.source = source;
        this.instance = instance;
        this.entity = entity;
        this.position = position;
    }

    /**
     * The player spawning the entity, or null if it is not spawned directly.
     */
    public @Nullable Player getSource() {
        return source;
    }

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    public void setEntity(@NotNull Entity entity) {
        this.entity = entity;
    }

    public @NotNull Pos getPosition() {
        return position;
    }

    public void setPosition(@NotNull Pos position) {
        this.position = position;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
