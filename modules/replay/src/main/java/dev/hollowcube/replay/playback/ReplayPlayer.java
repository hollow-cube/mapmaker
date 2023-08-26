package dev.hollowcube.replay.playback;

import dev.hollowcube.replay.Replay;
import dev.hollowcube.replay.change.RecordedPlayerMove;
import dev.hollowcube.replay.change.RecordedPlayerSpawn;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minestom.server.Viewable;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReplayPlayer implements Viewable {
    private final Replay replay;
    private final Instance instance;
    private final Set<Player> viewers = Collections.synchronizedSet(new HashSet<>());

    private final Int2ObjectMap<Entity> entities = new Int2ObjectArrayMap<>();

    private boolean playing = false;
    private Task playTask = null;

    private int tick = 0;

    public ReplayPlayer(@NotNull Replay replay, @NotNull Instance instance) {
        this.replay = replay;
        this.instance = instance;
    }

    public @NotNull Replay replay() {
        return replay;
    }

    public @NotNull Instance instance() {
        return instance;
    }

    public void start() {
        Check.stateCondition(playing, "Replay is already playing");
        playing = true;

        this.playTask = instance.scheduler().submitTask(this::tick);
    }

    public void stop() {
        if (!playing) return;
        playing = false;

        this.playTask.cancel();
        this.playTask = null;

        for (var entity : entities.values())
            entity.remove();
        entities.clear();

    }

    public int position() {
        return tick;
    }
    public int length() {
        return replay.getChanges().size();
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        viewers.add(player);
        for (var entity : entities.values()) {
            entity.addViewer(player);
        }
        return true;
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        viewers.remove(player);
        for (var entity : entities.values()) {
            entity.removeViewer(player);
        }
        return true;
    }

    @Override
    public @NotNull Set<@NotNull Player> getViewers() {
        return viewers;
    }

    private @NotNull TaskSchedule tick() {
        if (position() >= length()) {
            stop();
            return TaskSchedule.stop();
        }

        for (var change : replay.getChanges().get(position())) {
            switch (change) {
                case RecordedPlayerSpawn spawn -> {
                    var entity = new PlaybackPlayer(spawn.username(), spawn.skinTexture(), spawn.skinSignature());
                    entity.setAutoViewable(false);
                    entity.setInstance(instance(), spawn.pos());
                    initEntity(spawn.entityId(), entity);
                }
                case RecordedPlayerMove move -> {
                    var entity = entities.get(move.entityId());
                    if (entity == null) {
                        throw new RuntimeException("No such entity " + move.entityId());
                    }
                    entity.teleport(move.pos());
                    entity.sendPacketToViewers(new EntityHeadLookPacket(entity.getEntityId(), move.pos().yaw()));
                }
                default -> throw new IllegalStateException("Unexpected value: " + change);
            }
        }

        tick++;
        return TaskSchedule.nextTick();
    }

    private void initEntity(int entityId, @NotNull Entity entity) {
        entities.put(entityId, entity);
        for (var viewer : viewers) {
            entity.addViewer(viewer);
        }
    }
}
