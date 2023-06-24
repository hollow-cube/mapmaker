package net.hollowcube.map.feature.checkpoint;

import com.google.auto.service.AutoService;
import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapWorldCheckpointReachedEvent;
import net.hollowcube.map.event.MapWorldPlayerStartPlayingEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.BlockItemHandler;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.lang.MapMessages;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class CheckpointFeatureProvider implements FeatureProvider {
    private static final CheckpointPlateBlock CHECKPOINT_PLATE_BLOCK = new CheckpointPlateBlock();
    private static final ItemHandler CHECKPOINT_PLATE_ITEM = new BlockItemHandler(
            CHECKPOINT_PLATE_BLOCK, Block.HEAVY_WEIGHTED_PRESSURE_PLATE);

    private static final FinishPlateBlock FINISH_PLATE_BLOCK = new FinishPlateBlock();
    private static final ItemHandler FINISH_PLATE_ITEM = new BlockItemHandler(
            FINISH_PLATE_BLOCK, Block.LIGHT_WEIGHTED_PRESSURE_PLATE);

    // Marks the current reset height of a player. If they fall below it, they will be returned to their latest checkpoint, or the map spawn point.
    private static final Tag<Integer> RESET_HEIGHT_TAG = Tag.Integer("mapmaker:checkpoint/reset_height");
    public static final int MINIMUM_RESET_HEIGHT = -64;

    private final EventNode<InstanceEvent> resetManagementNode = EventNode.type("mapmaker:feature/checkpoint", EventFilter.INSTANCE)
            .addListener(MapWorldPlayerStartPlayingEvent.class, this::acceptPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::cleanupPlayer)
            .addListener(MapWorldCheckpointReachedEvent.class, this::handleCheckpointUpdate)
            .addListener(InstanceTickEvent.class, this::tick);

    @Override
    public void init(@NotNull ConfigProvider config) {
        MinecraftServer.getBlockManager().registerHandler(CHECKPOINT_PLATE_BLOCK.getNamespaceId(), () -> CHECKPOINT_PLATE_BLOCK);
        MinecraftServer.getBlockManager().registerHandler(FINISH_PLATE_BLOCK.getNamespaceId(), () -> FINISH_PLATE_BLOCK);
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0) {
            world.itemRegistry().register(CHECKPOINT_PLATE_ITEM);
            world.itemRegistry().register(FINISH_PLATE_ITEM);
        }

        if ((world.flags() & MapWorld.FLAG_PLAYING) != 0) {
            world.addScopedEventNode(resetManagementNode);
        }

        return true;
    }


    private void acceptPlayer(@NotNull MapWorldPlayerStartPlayingEvent event) {
        var player = event.getPlayer();
        var saveState = SaveState.fromPlayer(player);

        player.setTag(RESET_HEIGHT_TAG, getCheckpointResetHeight(event.getMap(), saveState.getCheckpoint()));
    }

    private void cleanupPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();

        player.removeTag(RESET_HEIGHT_TAG);
    }

    private void handleCheckpointUpdate(@NotNull MapWorldCheckpointReachedEvent event) {
        var player = event.getPlayer();
        var saveState = SaveState.fromPlayer(player);
//        if (event.getCheckpoint().getId().equals(saveState.getCheckpoint())) return; // Already at this checkpoint

        // Reached a new checkpoint
//        saveState.setCheckpoint(event.getCheckpoint().getId());
        player.setTag(RESET_HEIGHT_TAG, getCheckpointResetHeight(event.getMap(), saveState.getCheckpoint()));
        player.sendMessage(MapMessages.CHECKPOINT_REACHED);
    }

    private void tick(@NotNull InstanceTickEvent event) {
        var instance = event.getInstance();

        var players = instance.getEntityTracker().entities(EntityTracker.Target.PLAYERS);
        for (var player : players) {
            if (!MapHooks.isPlayerPlaying(player)) continue; // Player is not playing the map
            var map = MapWorld.forPlayer(player).map();

            var resetHeight = player.getTag(RESET_HEIGHT_TAG);
            if (resetHeight == null) continue; // No reset height set (something went wrong probably)

            if (player.getPosition().y() < resetHeight || player.getPosition().y() < MINIMUM_RESET_HEIGHT) {
                // Player has fallen below their reset height, return them to their latest checkpoint
                var saveState = SaveState.fromPlayer(player);
                var checkpoint = saveState.getCheckpoint();
                if (checkpoint == null) {
                    // No checkpoint set, return to spawn
//                    player.teleport(map.getSpawnPoint())
//                            .exceptionally(FutureUtil::handleException);
                } else {
                    // Return to checkpoint
//                    var checkpointPos = map.getPoi(checkpoint).getPos();
//                    player.teleport(Pos.fromPoint(checkpointPos))
//                            .exceptionally(FutureUtil::handleException);
                }
            }
        }
    }

    private int getCheckpointResetHeight(@NotNull MapData map, @NotNull String checkpointId) {
//        var checkpoint = map.getPoi(checkpointId);
//        if (checkpoint != null && checkpoint.getOrDefault("active", false)) {
//            return checkpoint.getOrDefault("resetHeight", checkpoint.getPos().blockY() - 5);
//        }

        // No reset height or its disabled, return the default reset height (min)
        //todo should be map height - 50 when map height is implemented
        return MINIMUM_RESET_HEIGHT - 50;
    }
}
