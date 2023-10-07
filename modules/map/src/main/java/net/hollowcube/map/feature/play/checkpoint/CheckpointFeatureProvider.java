package net.hollowcube.map.feature.play.checkpoint;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapPlayerResetTriggerEvent;
import net.hollowcube.map.event.MapWorldCheckpointReachedEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.lang.MapMessages;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.SaveState;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class CheckpointFeatureProvider implements FeatureProvider {

    // Marks the current reset height of a player. If they fall below it, they will be returned to their latest checkpoint, or the map spawn pos.
    private static final Tag<Integer> RESET_HEIGHT_TAG = Tag.Integer("mapmaker:checkpoint/reset_height");
    public static final int MINIMUM_RESET_HEIGHT = -64;

    private final EventNode<InstanceEvent> resetManagementNode = EventNode.type("mapmaker:feature/checkpoint", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::acceptPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::cleanupPlayer)
            .addListener(MapWorldCheckpointReachedEvent.class, this::handleCheckpointUpdate)
            .addListener(InstanceTickEvent.class, this::tick)
            .addListener(MapPlayerResetTriggerEvent.class, this::handlePlayerReset);

    @Override
    public @NotNull List<BlockHandler> blockHandlers() {
        return List.of(FinishPlateBlock.INSTANCE, CheckpointPlateBlock.INSTANCE);
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0) {
            world.itemRegistry().register(FinishPlateBlock.ITEM);
            world.itemRegistry().register(CheckpointPlateBlock.ITEM);
        }

        if ((world.flags() & MapWorld.FLAG_PLAYING) != 0) {
            world.addScopedEventNode(resetManagementNode);

            // Create checkpoint cache (which adds itself to the world)
            new CheckpointCache(world);
        }

        return true;
    }


    private void acceptPlayer(@NotNull MapPlayerInitEvent event) {
        if (!event.isFirstInit()) return;

        var player = event.getPlayer();
        var saveState = SaveState.fromPlayer(player);

        player.setTag(RESET_HEIGHT_TAG, 30);
//        player.setTag(RESET_HEIGHT_TAG, getCheckpointResetHeight(event.getMap(), saveState.checkpoint()));
    }

    private void cleanupPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();

        player.removeTag(RESET_HEIGHT_TAG);
    }

    private void handleCheckpointUpdate(@NotNull MapWorldCheckpointReachedEvent event) {
        var player = event.getPlayer();
        var saveState = SaveState.fromPlayer(player);
        if (event.checkpointId().equals(saveState.checkpoint())) return; // Already at this checkpoint

        // Reached a new checkpoint
        saveState.setCheckpoint(event.checkpointId(), player.getPosition());
//        player.setTag(RESET_HEIGHT_TAG, getCheckpointResetHeight(event.getMap(), saveState.checkpoint()));
        player.sendMessage(MapMessages.CHECKPOINT_REACHED);
    }

    private void tick(@NotNull InstanceTickEvent event) {
        var instance = event.getInstance();

        var players = instance.getEntityTracker().entities(EntityTracker.Target.PLAYERS);
        for (var player : players) {
            if (!MapHooks.isPlayerPlaying(player)) continue; // Player is not playing the map
            var world = MapWorld.forPlayer(player);

            var resetHeight = player.getTag(RESET_HEIGHT_TAG);
            if (resetHeight == null) continue; // No reset height set (something went wrong probably)

            if (player.getPosition().y() < resetHeight || player.getPosition().y() < MINIMUM_RESET_HEIGHT) {
                // Player has fallen below their reset height, return them to their latest checkpoint
                EventDispatcher.call(new MapPlayerResetTriggerEvent(world, player));
            }
        }
    }

    private void handlePlayerReset(@NotNull MapPlayerResetTriggerEvent event) {
        var player = event.getPlayer();
        var map = event.getMap();

        CompletableFuture<Void> future;

        var saveState = SaveState.fromPlayer(player);
        var checkpointId = saveState.checkpoint();
        if (checkpointId == null) {
            // No checkpoint set, return to spawn
            future = player.teleport(map.settings().getSpawnPoint());
            saveState.setPlaytime(0);
            saveState.setPlayStartTime(System.currentTimeMillis());
            saveState.setCompleted(false);
        } else {
            // Return to savestate at checkpoint
            Pos pos = saveState.checkpointPos();
            if (pos != null) {
                future = player.teleport(pos);
            } else {
                var cps = CheckpointCache.forInstance(event.getInstance());
                var checkpoint = cps.getCheckpoint(checkpointId);
                future = player.teleport(player.getPosition().withCoord(checkpoint.pos().add(0.5, 0, 0.5)));
            }
        }

        future.thenAccept(unused -> EventDispatcher.call(new MapPlayerInitEvent(event.mapWorld(), player, false)))
                .exceptionally(FutureUtil::handleException);
    }

    private int getCheckpointResetHeight(@NotNull MapData map, @Nullable String checkpointId) {
        var checkpoint = map.getObject(checkpointId);
        if (checkpoint == null)
            System.out.println("Could not find checkpoint with id " + checkpointId +
                    " in object list " + map.objects().toString());
//        var checkpoint = map.getPoi(checkpointId);
//        if (checkpoint != null && checkpoint.getOrDefault("active", false)) {
//            return checkpoint.getOrDefault("resetHeight", checkpoint.getPos().blockY() - 5);
//        }

        // No reset height or its disabled, return the default reset height (min)
        //todo should be map height - 50 when map height is implemented
        return MINIMUM_RESET_HEIGHT - 50;
    }
}
