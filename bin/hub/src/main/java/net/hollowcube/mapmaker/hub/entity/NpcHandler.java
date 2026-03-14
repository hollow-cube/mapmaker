package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public interface NpcHandler {
    Tag<NpcHandler> TAG = Tag.Transient("mapmaker:hub/npc_handler");

    void handlePlayerInteract(Player player, BaseNpcEntity npc, PlayerHand hand, boolean isLeftClick);

    EventNode<InstanceEvent> EVENT_NODE = EventNode.type("mapmaker:hub/npc_handler", EventFilter.INSTANCE)
            .addListener(PlayerEntityInteractEvent.class, event -> {
                if (!(event.getTarget() instanceof BaseNpcEntity npc)) return;
                if (!npc.hasTag(TAG)) return;

                npc.getTag(TAG).handlePlayerInteract(event.getPlayer(), npc, event.getHand(), false);
            });
    // Left click is disabled everywhere for now, its pretty annoying.
//            .addListener(EntityAttackEvent.class, event -> {
//                if (!(event.getTarget() instanceof BaseNpcEntity npc)) return;
//                if (!(event.getEntity() instanceof Player p)) return;
//                if (!npc.hasTag(TAG)) return;
//
//                npc.getTag(TAG).handlePlayerInteract(p, npc, Player.Hand.MAIN, true);
//            });

}
