package net.hollowcube.common.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class PlayerMoveVehicleEvent implements PlayerInstanceEvent, CancellableEvent {

    private final @NotNull Player player;
    private Pos newPosition;

    private boolean cancelled;

    public PlayerMoveVehicleEvent(@NotNull Player player, @NotNull Pos newPosition) {
        this.player = player;
        this.newPosition = newPosition;
    }

    /**
     * Gets the target position.
     *
     * @return the new position
     */
    public @NotNull Pos getNewPosition() {
        return newPosition;
    }

    /**
     * Changes the target position.
     *
     * @param newPosition the new target position
     */
    public void setNewPosition(@NotNull Pos newPosition) {
        this.newPosition = newPosition;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    @ApiStatus.Internal
    @SuppressWarnings("UnstableApiUsage")
    public static void post(@NotNull ClientVehicleMovePacket packet, @NotNull Player player) {
        var event = new PlayerMoveVehicleEvent(player, packet.position());
        EventDispatcher.call(event);
        if (event.isCancelled()) return;

        // Taken from PlayerVehicleListener
        final Entity vehicle = player.getVehicle();
        if (vehicle == null) return;

        vehicle.refreshPosition(event.getNewPosition());
    }
}
