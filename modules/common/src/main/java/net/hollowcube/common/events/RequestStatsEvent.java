package net.hollowcube.common.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.client.play.ClientStatusPacket;
import org.jetbrains.annotations.ApiStatus;

public record RequestStatsEvent(Player player) implements PlayerInstanceEvent {

    @Override
    public Player getPlayer() {
        return player;
    }

    @ApiStatus.Internal
    public static void post(ClientStatusPacket packet, Player player) {
        switch (packet.action()) {
            case REQUEST_STATS -> EventDispatcher.call(new RequestStatsEvent(player));
            case PERFORM_RESPAWN -> player.respawn();
        }
    }

}
