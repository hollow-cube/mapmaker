package net.hollowcube.compat.lunar.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

public interface LunarPlayerEvent extends PlayerEvent {

    Player player();

    @Override
    default Player getPlayer() {
        return player();
    }
}
