package net.hollowcube.mapmaker.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.compat.axiom.events.AxiomMarkerDataRequestEvent;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.action.gui.ActionEditorView;
import net.hollowcube.mapmaker.map.action.impl.TeleportAction;
import net.hollowcube.mapmaker.map.block.custom.*;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.event.entity.MarkerEntityEnteredEvent;
import net.hollowcube.mapmaker.map.event.entity.MarkerEntityExitedEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointPostChangeEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointPreChangeEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerStatusChangeEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.feature.edit.item.BuilderMenuItem;
import net.hollowcube.mapmaker.map.feature.edit.item.DisplayEntityItem;
import net.hollowcube.mapmaker.map.feature.edit.item.EnterTestModeItem;
import net.hollowcube.mapmaker.map.feature.edit.item.SpawnPointItem;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectDataV2;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.panels.Panel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPickBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.ItemBlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@AutoService(FeatureProvider.class)
public class CustomBlocksFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("custom-blocks-event-node", EventFilter.INSTANCE)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::cleanupPlayer)
            .addListener(AxiomMarkerDataRequestEvent.class, this::handleMarkerClick)
            .addListener(PlayerPickBlockEvent.class, this::handlePickBlock);
    private final EventNode<InstanceEvent> playingNode = EventNode.type("custom-blocks-event-node", EventFilter.INSTANCE)
            .addListener(MarkerEntityEnteredEvent.class, this::handleEffectMarkerEnter)
            .addListener(MarkerEntityExitedEvent.class, this::handleEffectMarkerExit);


    @Override
    public @NotNull List<Supplier<BlockHandler>> blockHandlers() {
        return List.of(
                FinishPlateBlock::new,
                CheckpointPlateBlock::new,
                BouncePadBlock::new,
                StatusPlateBlock::new
        );
    }

    @Override
    public void preinitMap(@NotNull MapWorld world) {
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return;

//        world.objectEntityHandlers().registerForMarkers(MapLeaderboardMarkerHandler.ID, MapLeaderboardMarkerHandler::new);
//        world.objectEntityHandlers().registerForMarkers(BouncePadMarkerHandler.ID, BouncePadMarkerHandler::new);
//        world.objectEntityHandlers().registerForMarkers(HappyGhastMarkerHandler.ID, HappyGhastMarkerHandler::new);
        world.objectEntityHandlers().register(ResetMarkerHandler.ID, ResetMarkerHandler::new);
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (world instanceof EditingMapWorld) {
            world.itemRegistry().register(FinishPlateBlock.ITEM);
            world.itemRegistry().register(CheckpointPlateBlock.ITEM);
            world.itemRegistry().register(BouncePadBlock.ITEM);
            world.itemRegistry().register(StatusPlateBlock.ITEM);

            //todo this shouldnt be registered from here
            world.itemRegistry().register(BuilderMenuItem.INSTANCE);
            world.itemRegistry().register(EnterTestModeItem.INSTANCE);
            world.itemRegistry().register(SpawnPointItem.INSTANCE);
            world.itemRegistry().register(DisplayEntityItem.INSTANCE);

            world.eventNode().addChild(eventNode);

            return true;
        } else if (world instanceof PlayingMapWorld || world instanceof TestingMapWorld) {
            world.eventNode().addChild(playingNode);
            return true;
        }

        return false;
    }

    private void cleanupPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();
        player.removeTag(BaseEffectData.TARGET_PLATE);
    }

    private void handleMarkerClick(@NotNull AxiomMarkerDataRequestEvent event) {
        var player = event.getPlayer(); // If sneaking allow edit like normal
        if (event.getData() == null || player.isSneaking()) return;

        if (!(event.marker() instanceof MarkerEntity marker)) return; // Sanity
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return; // Sanity

        switch (event.getData().getString("type")) {
            case "mapmaker:checkpoint" -> {
                // Block the edit and open the checkpoint editor gui
                event.setCancelled(true);
                var checkpointData = Objects.requireNonNullElseGet(marker.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG), CheckpointEffectDataV2::new);
                var actionLocation = marker.getPosition().withY(y -> y + Objects.requireNonNullElse(marker.getMin(), Pos.ZERO).y());
                var host = Panel.open(player, new ActionEditorView(checkpointData.actions(), "Checkpoint"));
                host.setTag(ActionEditorView.ACTION_LOCATION, actionLocation);
                host.setTag(TeleportAction.SPC_TAG, marker);
                host.onClose(() -> marker.setTag(CheckpointPlateBlock.ENTITY_DATA_TAG, checkpointData));
            }
            case "mapmaker:status" -> {
                // Block the edit and open the status plate editor gui
                event.setCancelled(true);
                var statusData = Objects.requireNonNullElseGet(marker.getTag(StatusPlateBlock.ENTITY_DATA_TAG), StatusEffectData::new);
                var actionLocation = marker.getPosition().withY(y -> y + Objects.requireNonNullElse(marker.getMin(), Pos.ZERO).y());
                var host = Panel.open(player, new ActionEditorView(statusData.actions(), "Status"));
                host.setTag(ActionEditorView.ACTION_LOCATION, actionLocation);
                host.setTag(TeleportAction.SPC_TAG, marker);
                host.onClose(() -> marker.setTag(StatusPlateBlock.ENTITY_DATA_TAG, statusData));
            }
            case "mapmaker:finish" -> {
                // Block the edit and do nothing there are no finish plate settings yet :)
                event.setCancelled(true);
            }
        }
    }

    private void handleEffectMarkerEnter(@NotNull MarkerEntityEnteredEvent event) {
        var marker = event.getMarkerEntity();
        var world = event.getMapWorld();
        var player = event.getPlayer();
        switch (marker.getType()) {
            case "mapmaker:checkpoint" -> {
                var checkpoint = marker.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG);
                if (checkpoint == null) return;

                var checkpointId = marker.getUuid().toString();
                world.callEvent(new MapPlayerCheckpointPreChangeEvent(player, world, checkpointId, checkpoint));
            }
            case "mapmaker:status" -> {
                if (StatusPlateBlock.APPLY_COOLDOWN.test(player)) {
                    var status = marker.getTag(StatusPlateBlock.ENTITY_DATA_TAG);
                    if (status == null) return;

                    var statusId = marker.getUuid().toString();
                    world.callEvent(new MapPlayerStatusChangeEvent(player, world, statusId, status));
                }
            }
            case "mapmaker:finish" -> {
                var finishId = marker.getUuid().toString();
                world.callEvent(new MapPlayerCompleteMapEvent(player, world, finishId));
            }
        }
    }

    private void handleEffectMarkerExit(@NotNull MarkerEntityExitedEvent event) {
        var marker = event.getMarkerEntity();
        var world = event.getMapWorld();
        var player = event.getPlayer();
        if (marker.getType().equals("mapmaker:checkpoint")) {
            var checkpoint = marker.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG);
            if (checkpoint == null) return;

            var checkpointId = marker.getUuid().toString();
            world.callEvent(new MapPlayerCheckpointPostChangeEvent(player, world, checkpointId, checkpoint));
        }
    }

    public void handlePickBlock(@NotNull PlayerPickBlockEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) return; // Sanity
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return; // Sanity

        // First try to get the block from the item registry
        var block = event.getBlock();
        var itemStack = world.itemRegistry().getItemStack(block, event.isIncludeData());

        // Otherwise create the item stack from the block
        if (itemStack == null) {
            var material = BlockUtil.getItem(block);
            if (material == null) return; // Sanity
            var builder = ItemStack.builder(material);
            if (event.isIncludeData() && !block.properties().isEmpty()) {
                builder.set(DataComponents.BLOCK_STATE, new ItemBlockState(block.properties()));
                builder.set(DataComponents.LORE, block.properties().entrySet().stream()
                        .<Component>map(entry -> Component.text()
                                .decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(entry.getKey(), NamedTextColor.GRAY))
                                .append(Component.text("=", NamedTextColor.DARK_GRAY))
                                .append(Component.text(entry.getValue(), NamedTextColor.WHITE))
                                .build())
                        .toList());
                builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
            itemStack = builder.build();
        }

        // Still no item, nothing to do
        if (itemStack == null) return;
        var inventory = player.getInventory();

        // If the item is already on the hotbar, swap to it
        for (int i = 0; i < 9; i++) {
            if (!inventory.getItemStack(i).isSimilar(itemStack))
                continue;
            player.setHeldItemSlot((byte) i);
            break;
        }

        int targetSlot = player.getHeldSlot();
        var targetItem = inventory.getItemStack(targetSlot);
        if (targetItem.isSimilar(itemStack)) return;
        if (!targetItem.isAir()) {
            // Try to find an empty slot
            for (int i = 0; i < 9; i++) {
                if (inventory.getItemStack(i).isAir()) {
                    targetSlot = i;
                    break;
                }
            }
            // If we didnt find an empty slot its fine we can keep the original and replace.
        }

        // If the item already exists in the inventory, swap to it
        int existingSlot = -1;
        for (int i = 9; i < inventory.getSize(); i++) {
            if (inventory.getItemStack(i).isSimilar(itemStack)) {
                existingSlot = i;
                break;
            }
        }

        if (existingSlot != -1) {
            var existingItem = inventory.getItemStack(existingSlot);
            inventory.setItemStack(existingSlot, itemStack);
            inventory.setItemStack(targetSlot, existingItem);
        } else {
            inventory.setItemStack(targetSlot, itemStack);
            if (targetSlot != player.getHeldSlot()) {
                player.setHeldItemSlot((byte) targetSlot);
            }
        }
    }

}
