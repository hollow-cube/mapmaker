package net.hollowcube.compat.axiom.events;

import net.hollowcube.compat.axiom.packets.serverbound.AxiomServerboundModifyEntitiesPacket;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Called when an entity is trying to be modified by Axiom, this is used to allow different modules to handle the modification of entities.
 * <p>
 * When a module handles this event, it should set the {@link #handled} flag to true to prevent other modules from handling it.
 */
public final class AxiomTryModifyEntityEvent implements AxiomEvent {

    private final @NotNull Player player;
    private final @NotNull Entity entity;
    private final @Nullable Pos pos;

    private final @Nullable CompoundBinaryTag nbt;

    private final @NotNull AxiomServerboundModifyEntitiesPacket.PassengerChange change;
    private final @NotNull List<UUID> passengers;

    private boolean handled;

    public AxiomTryModifyEntityEvent(
            @NotNull Player player,
            @NotNull Entity entity,
            @Nullable Pos pos,
            @Nullable CompoundBinaryTag nbt,
            @NotNull AxiomServerboundModifyEntitiesPacket.PassengerChange change,
            @NotNull List<UUID> passengers
    ) {
        this.player = player;
        this.entity = entity;
        this.pos = pos;
        this.nbt = nbt;
        this.change = change;
        this.passengers = passengers;
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    public @NotNull Entity entity() {
        return entity;
    }

    public @Nullable Pos pos() {
        return pos;
    }

    public @Nullable CompoundBinaryTag nbt() {
        return nbt;
    }

    public @NotNull AxiomServerboundModifyEntitiesPacket.PassengerChange change() {
        return change;
    }

    public @NotNull List<UUID> passengers() {
        return passengers;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

}
