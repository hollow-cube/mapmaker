package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import dev.hollowcube.replay.Replay;
import dev.hollowcube.replay.playback.ReplayPlayer;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(FeatureProvider.class)
public class GhostFeatureProvider implements FeatureProvider {
    private static final Logger logger = LoggerFactory.getLogger(RecordingFeatureProvider.class);

    private final Tag<Replay> BEST_REPLAY_TAG = Tag.Transient("mapmaker:play/best_replay");
    private final Tag<ReplayPlayer> PLAYBACK_TAG = Tag.Transient("mapmaker:play/replay_playback");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/ghost_playback", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
//        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0)
//            return false;
//
//        world.addScopedEventNode(eventNode);

        return false;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        try {
            var player = event.getPlayer();
            var playerData = PlayerDataV2.fromPlayer(player);
            var world = event.mapWorld();

            var oldPlayback = player.getTag(PLAYBACK_TAG);
            if (oldPlayback != null) {
                oldPlayback.stop();
                player.removeTag(PLAYBACK_TAG);
            }

            Replay replay = null;
            if (event.isFirstInit()) {
                var mapService = world.server().mapService();
                var bestSaveState = mapService.getBestSaveState(world.map().id(), playerData.id());
                if (bestSaveState != null) {
                    var replayData = mapService.getSaveStateReplay(world.map().id(), playerData.id(), bestSaveState.id());
                    if (replayData != null) {
                        replay = Replay.read(RecordingFeatureProvider.replayFactory, replayData);
                        player.setTag(BEST_REPLAY_TAG, replay);
                    }
                }
            } else {
                replay = player.getTag(BEST_REPLAY_TAG);
            }

            if (replay == null) return;

            var playback = new ReplayPlayer(replay, player.getInstance());
            playback.addViewer(player);
            playback.start();
            player.setTag(PLAYBACK_TAG, playback);

        } catch (Exception e) {
            logger.info("failed to load replay", e);
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

}
