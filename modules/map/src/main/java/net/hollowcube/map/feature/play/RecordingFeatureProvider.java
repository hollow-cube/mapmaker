package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import dev.hollowcube.replay.ReplayFactory;
import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.change.RecordedPlayerMove;
import dev.hollowcube.replay.change.RecordedPlayerSpawn;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(FeatureProvider.class)
public class RecordingFeatureProvider implements FeatureProvider {
    private static final Logger logger = LoggerFactory.getLogger(RecordingFeatureProvider.class);

    private record State(ReplayRecorder recorder, Task task) {
    }

    private static final Tag<State> STATE = Tag.Transient("mapmaker:play/recording_state");

    public static final ReplayFactory replayFactory = ReplayFactory.builder()
            .build();

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/recording", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerCompleteMapEvent.class, this::handleMapCompletion);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // DISABLE FOR NOW IT IS VERY BUGGY
        if (true) return false;

        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0)
            return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return false;

        world.addScopedEventNode(eventNode);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        var oldState = player.getTag(STATE);
        if (oldState != null) {
            player.removeTag(STATE);
            oldState.task.cancel();
            oldState.recorder.close();
        }

        var instance = event.getInstance();
        var recorder = new ReplayRecorder(replayFactory, instance, player.getPosition());

        var skin = player.getSkin();
        String skinTexture = null, skinSignature = null;
        if (skin != null) {
            skinTexture = skin.textures();
            skinSignature = skin.signature();
        }

        recorder.record(new RecordedPlayerSpawn(
                player.getEntityId(), player.getUsername(),
                skinTexture, skinSignature,
                player.getPosition()
        ));

        var task = player.scheduler().submitTask(() -> {
            recorder.record(new RecordedPlayerMove(player.getEntityId(), player.getPosition()));

            return TaskSchedule.nextTick();
        });
        player.setTag(STATE, new State(recorder, task));

    }

    private void handleMapCompletion(@NotNull MapPlayerCompleteMapEvent event) {
        var player = event.getPlayer();
        var state = player.getTag(STATE);
        if (state == null) return;

        player.removeTag(STATE);
        var is = state.recorder.toStream();

        Thread.startVirtualThread(() -> {
            try {
                var playerData = PlayerDataV2.fromPlayer(player);
                var saveState = SaveState.fromPlayer(player);
                var world = event.getMapWorld();
                var mapService = world.server().mapService();
                mapService.updateSaveStateReplay(world.map().id(), playerData.id(), saveState.id(), is);
            } catch (Exception e) {
                logger.error("Failed to save replay", e);
            }
        });
    }

}
