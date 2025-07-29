package net.hollowcube.mapmaker.map.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface Map2PlayerEvent extends Map2Event, PlayerInstanceEvent {

    Player player(); // Exists for record convenience

    @Override
    default Instance getInstance() {
        return Map2Event.super.getInstance();
    }

    @Override
    default Player getPlayer() {
        return player();
    }
}
