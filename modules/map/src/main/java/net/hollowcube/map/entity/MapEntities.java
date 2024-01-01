package net.hollowcube.map.entity;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.entity.impl.ItemFrameEntity;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public final class MapEntities {

    public static void init(@NotNull EventNode<InstanceEvent> eventNode) {
        eventNode.addListener(PlayerEntityInteractEvent.class, MapEntities::handleEntityInteract);
        eventNode.addListener(EntityAttackEvent.class, MapEntities::handleEntityAttack);

        MapEntityType.override(EntityType.ITEM_FRAME, ItemFrameEntity::new);
        MapEntityType.override(EntityType.GLOW_ITEM_FRAME, ItemFrameEntity.Glowing::new);
    }

    private static void handleEntityInteract(@NotNull PlayerEntityInteractEvent event) {
        if (!(event.getTarget() instanceof MapEntity mapEntity)) return;

        var player = event.getPlayer();
        if (!(MapHooks.isPlayerBuilding(player) || MapHooks.isPlayerPlaying(player))) return;

        var mapWorld = MapWorld.forPlayer(player);
        mapEntity.onRightClick(mapWorld, player, event.getHand(), event.getInteractPosition());
    }

    private static void handleEntityAttack(@NotNull EntityAttackEvent event) {
        if (!(event.getTarget() instanceof MapEntity mapEntity)) return;

        if (!(event.getEntity() instanceof Player player)) return;
        if (!(MapHooks.isPlayerBuilding(player) || MapHooks.isPlayerPlaying(player))) return;

        var mapWorld = MapWorld.forPlayer(player);
        mapEntity.onLeftClick(mapWorld, player);
    }
}
