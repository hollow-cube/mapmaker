package net.hollowcube.map2.entity;

import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.entity.impl.DisplayEntity;
import net.hollowcube.map2.entity.impl.EndCrystalEntity;
import net.hollowcube.map2.entity.impl.ItemFrameEntity;
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

        MapEntityType.override(EntityType.END_CRYSTAL, EndCrystalEntity::new);

        MapEntityType.override(EntityType.ITEM_DISPLAY, DisplayEntity::new);
        MapEntityType.override(EntityType.BLOCK_DISPLAY, DisplayEntity::new);
        MapEntityType.override(EntityType.TEXT_DISPLAY, DisplayEntity::new);
    }

    private static void handleEntityInteract(@NotNull PlayerEntityInteractEvent event) {
        if (!(event.getTarget() instanceof MapEntity mapEntity)) return;

        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        mapEntity.onRightClick(world, player, event.getHand(), event.getInteractPosition());
    }

    private static void handleEntityAttack(@NotNull EntityAttackEvent event) {
        if (!(event.getTarget() instanceof MapEntity mapEntity)) return;

        if (!(event.getEntity() instanceof Player player)) return;
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        mapEntity.onLeftClick(world, player);
    }
}
