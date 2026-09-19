package net.hollowcube.common.events;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.SignTextSlot;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record UpdateSignTextEvent(
        @NotNull Player player,
        @NotNull Point position,
        @NotNull SignTextSlot slot,
        @NotNull List<String> lines
) implements PlayerInstanceEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @ApiStatus.Internal
    public static void post(@NotNull ClientUpdateSignPacket packet, @NotNull Player player) {
        EventDispatcher.call(new UpdateSignTextEvent(player, packet.blockPosition(), packet.slot(), packet.lines()));
    }

}
