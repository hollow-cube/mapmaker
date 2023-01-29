package net.hollowcube.map.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapWorldCheckpointReachedEvent;
import net.hollowcube.map.event.MapWorldPlayerStartPlayingEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.SaveState;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
@AutoService(MapFeature.class)
public class CheckpointFeature implements MapFeature {
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:feature/checkpoint", EventFilter.INSTANCE)
            .addListener(MapWorldPlayerStartPlayingEvent.class, this::acceptPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::cleanupPlayer)
            .addListener(MapWorldCheckpointReachedEvent.class, this::handleCheckpointUpdate)
            .addListener(InstanceTickEvent.class, this::tick);

    // Marks the current reset height of a player. If they fall below it they will be returned to their latest checkpoint, or the map spawn point.
    private static final Tag<Integer> RESET_HEIGHT_TAG = Tag.Integer("mapmaker:checkpoint/reset_height");

    @Override
    public @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
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
        if (event.getCheckpoint().getId().equals(saveState.getCheckpoint())) return; // Already at this checkpoint

        // Reached a new checkpoint
        saveState.setCheckpoint(event.getCheckpoint().getId());
        player.setTag(RESET_HEIGHT_TAG, getCheckpointResetHeight(event.getMap(), saveState.getCheckpoint()));
        player.sendMessage(Component.translatable("play.checkpoint.reached"));
    }

    private void tick(@NotNull InstanceTickEvent event) {
        var instance = event.getInstance();
        var map = MapWorld.fromInstance(instance).map();

        var players = instance.getEntityTracker().entities(EntityTracker.Target.PLAYERS);
        for (var player : players) {
            if (!MapHooks.isPlayerPlaying(player)) continue; // Player is not playing the map

            var resetHeight = player.getTag(RESET_HEIGHT_TAG);
            if (resetHeight == null) continue; // No reset height set (something went wrong probably)

            if (player.getPosition().y() < resetHeight) {
                // Player has fallen below their reset height, return them to their latest checkpoint
                var saveState = SaveState.fromPlayer(player);
                var checkpoint = saveState.getCheckpoint();
                if (checkpoint == null) {
                    // No checkpoint set, return to spawn
                    player.teleport(map.getSpawnPoint())
                            .exceptionally(FutureUtil::handleException);
                } else {
                    // Return to checkpoint
                    var checkpointPos = map.getPoi(checkpoint).getPos();
                    player.teleport(Pos.fromPoint(checkpointPos))
                            .exceptionally(FutureUtil::handleException);
                }
            }
        }
    }

    private int getCheckpointResetHeight(@NotNull MapData map, @NotNull String checkpointId) {
        var checkpoint = map.getPoi(checkpointId);
        if (checkpoint != null && checkpoint.getOrDefault("active", false)) {
            return checkpoint.getOrDefault("resetHeight", checkpoint.getPos().blockY() - 5);
        }

        // No reset height or its disabled, return the default reset height (min)
        //todo should be map height - 50 when map height is implemented
        return -64 - 50;
    }

}
