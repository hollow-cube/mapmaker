package net.hollowcube.compat.axiom.events;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when an entity is trying to be spawned by Axiom, this is used to allow different modules to handle the spawning.
 * <p>
 * When a module handles this event, it should set the {@link #handled} flag to true to prevent other modules from handling it.
 */
public final class AxiomTrySpawnEntityEvent implements AxiomEvent {

    private final Player player;
    private final UUID uuid;
    private final @Nullable Entity copyFrom;
    private final Pos pos;
    private final @Nullable CompoundBinaryTag nbt;

    private boolean handled;

    public AxiomTrySpawnEntityEvent(
        Player player,
        UUID uuid,
        @Nullable Entity copyFrom,
        Pos pos,
        @Nullable CompoundBinaryTag nbt
    ) {
        this.player = player;
        this.uuid = uuid;
        this.copyFrom = copyFrom;
        this.pos = pos;
        this.nbt = nbt;
    }

    public AxiomTrySpawnEntityEvent(
        Player player,
        UUID uuid,
        @Nullable UUID copyFrom,
        Pos pos,
        @Nullable CompoundBinaryTag nbt
    ) {
        this(player, uuid, copyFrom != null ? player.getInstance().getEntityByUuid(copyFrom) : null, pos, nbt);
    }

    @Override
    public Player player() {
        return player;
    }

    public UUID uuid() {
        return uuid;
    }

    public @Nullable Entity copyFrom() {
        return copyFrom;
    }

    public Pos pos() {
        return pos;
    }

    public @Nullable CompoundBinaryTag nbt() {
        return nbt;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

}
