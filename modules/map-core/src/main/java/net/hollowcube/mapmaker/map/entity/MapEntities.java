package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.entity.impl.living.*;
import net.hollowcube.mapmaker.map.entity.impl.other.EndCrystalEntity;
import net.hollowcube.mapmaker.map.entity.impl.other.ItemFrameEntity;
import net.hollowcube.mapmaker.map.entity.impl.other.LeashKnotEntity;
import net.hollowcube.mapmaker.map.entity.impl.other.PaintingEntity;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public final class MapEntities {

    // TODO: no duplicate
    public static void initNoEvents() {
        MapEntityType.override(EntityType.ITEM_FRAME, ItemFrameEntity::new);
        MapEntityType.override(EntityType.GLOW_ITEM_FRAME, ItemFrameEntity.Glowing::new);
        MapEntityType.override(EntityType.PAINTING, PaintingEntity::new);
        MapEntityType.override(EntityType.ARMOR_STAND, ArmorStandEntity::new);
        MapEntityType.override(EntityType.END_CRYSTAL, EndCrystalEntity::new);
        MapEntityType.override(EntityType.LEASH_KNOT, LeashKnotEntity::new);

        MapEntityType.override(EntityType.PANDA, PandaEntity::new);
        MapEntityType.override(EntityType.FOX, FoxEntity::new);
        MapEntityType.override(EntityType.WOLF, WolfEntity::new);
        MapEntityType.override(EntityType.CHICKEN, ChickenEntity::new);
        MapEntityType.override(EntityType.SHULKER, ShulkerEntity::new);

        MapEntityType.override(EntityType.BLOCK_DISPLAY, DisplayEntity.Block::new);
        MapEntityType.override(EntityType.ITEM_DISPLAY, DisplayEntity.Item::new);
        MapEntityType.override(EntityType.TEXT_DISPLAY, DisplayEntity.Text::new);

        MapEntityType.override(EntityType.MARKER, MarkerEntity::new);
        MapEntityType.override(EntityType.INTERACTION, InteractionEntity::new);
    }

    public static void init(@NotNull EventNode<InstanceEvent> eventNode) {
        eventNode.addListener(PlayerEntityInteractEvent.class, MapEntities::handleEntityInteract);
        eventNode.addListener(EntityAttackEvent.class, MapEntities::handleEntityAttack);

        MapEntityType.override(EntityType.ITEM_FRAME, ItemFrameEntity::new);
        MapEntityType.override(EntityType.GLOW_ITEM_FRAME, ItemFrameEntity.Glowing::new);
        MapEntityType.override(EntityType.PAINTING, PaintingEntity::new);
        MapEntityType.override(EntityType.ARMOR_STAND, ArmorStandEntity::new);
        MapEntityType.override(EntityType.END_CRYSTAL, EndCrystalEntity::new);
        MapEntityType.override(EntityType.LEASH_KNOT, LeashKnotEntity::new);

        MapEntityType.override(EntityType.PANDA, PandaEntity::new);
        MapEntityType.override(EntityType.FOX, FoxEntity::new);
        MapEntityType.override(EntityType.WOLF, WolfEntity::new);
        MapEntityType.override(EntityType.CHICKEN, ChickenEntity::new);
        MapEntityType.override(EntityType.SHULKER, ShulkerEntity::new);

        MapEntityType.override(EntityType.BLOCK_DISPLAY, DisplayEntity.Block::new);
        MapEntityType.override(EntityType.ITEM_DISPLAY, DisplayEntity.Item::new);
        MapEntityType.override(EntityType.TEXT_DISPLAY, DisplayEntity.Text::new);

        MapEntityType.override(EntityType.MARKER, MarkerEntity::new);
        MapEntityType.override(EntityType.INTERACTION, InteractionEntity::new);
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
