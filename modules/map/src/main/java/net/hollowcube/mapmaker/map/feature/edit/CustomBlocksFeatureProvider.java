package net.hollowcube.mapmaker.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.axiom.events.AxiomMarkerDataRequestEvent;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.custom.*;
import net.hollowcube.mapmaker.map.block.custom.bouncepad.BouncePadMarkerHandler;
import net.hollowcube.mapmaker.map.entity.marker.MapLeaderboardMarkerHandler;
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
import net.hollowcube.mapmaker.map.gui.effect.EditCheckpointView;
import net.hollowcube.mapmaker.map.gui.effect.EditStatusView;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@AutoService(FeatureProvider.class)
public class CustomBlocksFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("custom-blocks-event-node", EventFilter.INSTANCE)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::cleanupPlayer)
            .addListener(AxiomMarkerDataRequestEvent.class, this::handleMarkerClick);
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

        world.objectEntityHandlers().registerForMarkers(MapLeaderboardMarkerHandler.ID, MapLeaderboardMarkerHandler::new);
        world.objectEntityHandlers().registerForMarkers(BouncePadMarkerHandler.ID, BouncePadMarkerHandler::new);
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
                var checkpointData = marker.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG);
                var maxResetHeight = (int) (Objects.requireNonNullElse(marker.getMin(), Pos.ZERO).y() + marker.getPosition().y());
                world.server().guiController().show(player, c -> new EditCheckpointView(c.with(Map.of("updateTarget", marker)),
                        checkpointData, maxResetHeight, () -> marker.setTag(CheckpointPlateBlock.ENTITY_DATA_TAG, checkpointData)));
            }
            case "mapmaker:status" -> {
                // Block the edit and open the status plate editor gui
                event.setCancelled(true);
                var statusData = marker.getTag(StatusPlateBlock.ENTITY_DATA_TAG);
                var maxResetHeight = (int) (Objects.requireNonNullElse(marker.getMin(), Pos.ZERO).y() + marker.getPosition().y());
                world.server().guiController().show(player, c -> new EditStatusView(c.with(Map.of("updateTarget", marker)),
                        statusData, maxResetHeight, () -> marker.setTag(StatusPlateBlock.ENTITY_DATA_TAG, statusData)));
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

}
