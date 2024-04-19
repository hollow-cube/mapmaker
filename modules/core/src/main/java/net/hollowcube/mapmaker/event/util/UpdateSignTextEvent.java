package net.hollowcube.mapmaker.event.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record UpdateSignTextEvent(
        @NotNull Player player,
        @NotNull Point blockPosition,
        boolean isFrontText,
        @NotNull List<String> lines
) implements PlayerInstanceEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public static void packetListener(@NotNull ClientUpdateSignPacket packet, @NotNull Player player) {
        EventDispatcher.call(new UpdateSignTextEvent(player, packet.blockPosition(), packet.isFrontText(), packet.lines()));
    }

}
