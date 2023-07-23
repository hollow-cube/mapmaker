package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import dev.hollowcube.replay.Replay;
import dev.hollowcube.replay.ReplayFactory;
import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.change.RecordedPlayerMove;
import dev.hollowcube.replay.change.RecordedPlayerSpawn;
import dev.hollowcube.replay.playback.PlaybackPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class RecordingFeatureProvider implements FeatureProvider {

    private record State(ReplayRecorder recorder, Task task) {}
    private static final Tag<State> STATE = Tag.Transient("mapmaker:play/recording_state");

    private final ReplayFactory replayFactory = ReplayFactory.builder()
            .build();

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/recording", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldCompleteEvent.class, this::handleMapCompletion);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0)
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
            oldState.recorder.complete();
            player.sendMessage("Recording stopped");

            doReplay(player, oldState.recorder.complete());
        }


        var instance = event.getInstance();
        var recorder = new ReplayRecorder(instance, player.getPosition());
        recorder.record(new RecordedPlayerSpawn(player.getEntityId()));
        var task = player.scheduler().submitTask(() -> {
            recorder.record(new RecordedPlayerMove(player.getEntityId(), player.getPosition()));

            return TaskSchedule.nextTick();
        });
        player.setTag(STATE, new State(recorder, task));

        player.sendMessage("Recording started");
    }

    private void handleMapCompletion(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();
        var state = player.getTag(STATE);
        if (state == null) return;

        player.removeTag(STATE);
        state.task.cancel();
        var replay = state.recorder.complete();
        player.sendMessage("Recording stopped");

        doReplay(player, replay);
    }

    private void doReplay(@NotNull Player player, @NotNull Replay rec) {
        var changeIter = rec.getChanges().iterator();

        var entitiesById = new Int2ObjectArrayMap<Entity>();

        player.scheduler().submitTask(() -> {
            if (!changeIter.hasNext()) {
                for (var entity : entitiesById.values()) {
                    entity.remove();
                }
                return TaskSchedule.stop();
            }

            for (var change : changeIter.next()) {
                switch (change) {
                    case RecordedPlayerSpawn spawn -> {
                        var entity = new PlaybackPlayer();
                        entity.setInstance(player.getInstance());
                        entitiesById.put(spawn.entityId(), entity);
                    }
                    case RecordedPlayerMove move -> {
                        var entity = entitiesById.get(move.entityId());
                        if (entity == null) {
                            throw new RuntimeException("No such entity " + move.entityId());
                        }
                        entity.teleport(move.pos());
                        entity.sendPacketToViewers(new EntityHeadLookPacket(entity.getEntityId(), move.pos().yaw()));
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + change);
                }
            }

            return TaskSchedule.nextTick();
        });
    }

}
