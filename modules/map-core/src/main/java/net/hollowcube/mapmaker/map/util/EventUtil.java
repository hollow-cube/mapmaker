package net.hollowcube.mapmaker.map.util;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Set;
import java.util.UUID;

@NotNullByDefault
public final class EventUtil {
    private static final EventFilter<PlayerInstanceEvent, ?> PLAYER_INSTANCE_FILTER =
            EventFilter.from(PlayerInstanceEvent.class, null, null);

    public static EventNode<PlayerInstanceEvent> playerEventNode(Set<Player> mutableSet) {
        return EventNode.event(UUID.randomUUID().toString(), PLAYER_INSTANCE_FILTER,
                event -> mutableSet.contains(event.getPlayer()));
    }

}
