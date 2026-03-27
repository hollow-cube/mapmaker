package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;

public record PlayerJumpEvent(MapPlayer player) implements PlayerInstanceEvent {
    @Override
    public Player getPlayer() {
        return player;
    }
}
