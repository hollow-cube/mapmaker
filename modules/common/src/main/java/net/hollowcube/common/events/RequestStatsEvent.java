package net.hollowcube.common.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.client.play.ClientStatusPacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public record RequestStatsEvent(
        @NotNull Player player
) implements PlayerInstanceEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @ApiStatus.Internal
    public static void post(@NotNull ClientStatusPacket packet, @NotNull Player player) {
        switch (packet.action()) {
            case REQUEST_STATS -> EventDispatcher.call(new RequestStatsEvent(player));
            case PERFORM_RESPAWN -> player.respawn();
        }
    }

}
