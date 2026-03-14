package net.hollowcube.common.events;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public record UpdateSignTextEvent(
        Player player,
        Point position,
        boolean isFrontText,
        List<String> lines
) implements PlayerInstanceEvent {

    @Override
    public Player getPlayer() {
        return player;
    }

    @ApiStatus.Internal
    public static void post(ClientUpdateSignPacket packet, Player player) {
        EventDispatcher.call(new UpdateSignTextEvent(player, packet.blockPosition(), packet.isFrontText(), packet.lines()));
    }

}
