package net.hollowcube.mapmaker.map.util;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Set;
import java.util.UUID;

@NotNullByDefault
public final class EventUtil {
    private static final EventFilter<PlayerInstanceEvent, ?> PLAYER_INSTANCE_FILTER =
            EventFilter.from(PlayerInstanceEvent.class, null, null);

    public static final EventNode<InstanceEvent> EVENT_NODE = EventNode.type("read_only_events", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
            .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
            .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true));

    public static EventNode<PlayerInstanceEvent> playerEventNode() {
        return EventNode.type(UUID.randomUUID().toString(), PLAYER_INSTANCE_FILTER);
    }

    public static EventNode<PlayerInstanceEvent> playerEventNode(Set<Player> mutableSet) {
        return EventNode.event(UUID.randomUUID().toString(), PLAYER_INSTANCE_FILTER,
                event -> mutableSet.contains(event.getPlayer()));
    }

}
