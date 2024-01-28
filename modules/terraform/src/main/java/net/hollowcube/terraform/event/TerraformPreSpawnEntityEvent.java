package net.hollowcube.terraform.event;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

public class TerraformPreSpawnEntityEvent implements InstanceEvent, CancellableEvent {
    private final Player source;
    private final Instance instance;
    private BiFunction<EntityType, UUID, Entity> constructor = TerraformPreSpawnEntityEvent::defaultConstructor;

    private boolean cancelled = false;

    public TerraformPreSpawnEntityEvent(@Nullable Player source, @NotNull Instance instance) {
        this.source = source;
        this.instance = instance;
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

    public @NotNull BiFunction<EntityType, UUID, Entity> getConstructor() {
        return constructor;
    }

    public void setConstructor(@NotNull BiFunction<EntityType, UUID, Entity> constructor) {
        this.constructor = constructor;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    private static @NotNull Entity defaultConstructor(@NotNull EntityType entityType, @NotNull UUID uuid) {
        return new Entity(entityType, uuid);
    }

}
