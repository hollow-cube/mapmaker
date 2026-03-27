package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Set;
import java.util.UUID;

@NotNullByDefault
public final class EventUtil {
    public static final EventFilter<PlayerInstanceEvent, ?> PLAYER_INSTANCE_FILTER =
            EventFilter.from(PlayerInstanceEvent.class, null, null);

    private static <T extends CancellableEvent & PlayerEvent> void cancelAndPing(T event) {
        event.setCancelled(true);
        // Send a ping when they drop their items so we can determine if they are out of sync.
        // For example: to prevent block placement in this case.
        ((MapPlayer) event.getPlayer()).ping();
    }

    public static final EventNode<InstanceEvent> READ_ONLY_NODE = EventNode.type("read_only_events", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
            .addListener(ItemDropEvent.class, EventUtil::cancelAndPing)
            .addListener(InventoryPreClickEvent.class, EventUtil::cancelAndPing)
            .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true));

    public static EventNode<PlayerInstanceEvent> playerEventNode() {
        return EventNode.type(UUID.randomUUID().toString(), PLAYER_INSTANCE_FILTER);
    }

    public static EventNode<PlayerInstanceEvent> playerEventNode(Set<Player> mutableSet) {
        return EventNode.event(UUID.randomUUID().toString(), PLAYER_INSTANCE_FILTER,
                event -> mutableSet.contains(event.getPlayer()));
    }

}
