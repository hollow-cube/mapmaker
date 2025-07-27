package net.hollowcube.mapmaker.map.block.ghost;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.mapmaker.map.util.PositionUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * <p>Handles per-player block states. Can be reset at will (eg on reset of a map). One manager exists per
 * player (and is created on demand).</p>
 *
 * <p>Note: block handlers and NBT are not necessarily preserved, so this should not be used where those are relevant.</p>
 *
 * <p>{@link GhostBlockHolder} is NOT thread-safe. It should be accessed from the player thread only.</p>
 */
public class GhostBlockHolder implements Block.Getter, Block.Setter {
    private static final Tag<GhostBlockHolder> TAG = Tag.Transient("ghost_block_manager");

    public static @Nullable GhostBlockHolder forPlayerOptional(@NotNull Player player) {
        var existing = player.getTag(TAG);
        if (existing != null) {
            if (existing.instance.equals(player.getInstance())) {
                return existing;
            }

            // Sanity check but it was old, so remove it.
            existing.clear();
            player.removeTag(TAG);
        }
        return null;
    }

    public static @NotNull GhostBlockHolder forPlayer(@NotNull Player player) {
        var existing = forPlayerOptional(player);
        if (existing != null) return existing;

        var manager = new GhostBlockHolder(player);
        player.setTag(TAG, manager);
        return manager;
    }

    public static void clear(@NotNull Player player, boolean delete) {
        var manager = forPlayerOptional(player);
        if (manager != null) manager.clear();
        if (delete) player.removeTag(TAG);
    }

    private final Player player;
    private final Instance instance;

    private Long2ObjectMap<Block> blocks = new Long2ObjectArrayMap<>();
    private Long2ObjectMap<TaskWrapper> tasks = new Long2ObjectArrayMap<>();

    private GhostBlockHolder(@NotNull Player player) {
        this.player = player;
        this.instance = player.getInstance();
    }

    /**
     * Returns the overridden block at the given position, or the block from the instance
     * if there is no override.
     *
     * @param x       the x coordinate
     * @param y       the y coordinate
     * @param z       the z coordinate
     * @param ignored always unused, see class javadoc
     * @return the block at the position (from instance if not overridden)
     */
    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Block.Getter.Condition ignored) {
        final Block overridden = blocks.get(PositionUtil.packPosition(x, y, z));
        return overridden != null ? overridden : instance.getBlock(x, y, z, Block.Getter.Condition.TYPE);
    }

    @Override
    public void setBlock(int x, int y, int z, @Nullable Block block) {
        setBlock(new Vec(x, y, z), block);
    }

    @Override
    public void setBlock(@NotNull Point blockPosition, @Nullable Block block) {
        if (block != null) {
            blocks.put(PositionUtil.packPosition(blockPosition), block);
            player.sendPacket(new BlockChangePacket(blockPosition, block));
        } else {
            blocks.remove(PositionUtil.packPosition(blockPosition));
            player.sendPacket(new BlockChangePacket(blockPosition, instance.getBlock(blockPosition, Condition.TYPE)));
        }
    }

    public void clear() {
        var oldTasks = this.tasks;
        this.tasks = new Long2ObjectArrayMap<>();
        var oldBlocks = this.blocks;
        this.blocks = new Long2ObjectArrayMap<>();

        oldTasks.values().forEach(TaskWrapper::cancel);
        for (var entry : oldBlocks.long2ObjectEntrySet()) {
            var blockPosition = PositionUtil.unpackPosition(entry.getLongKey());
            if (!instance.isChunkLoaded(blockPosition)) continue;

            var instanceBlock = instance.getBlock(blockPosition, Condition.TYPE);
            player.sendPacket(new BlockChangePacket(blockPosition, instanceBlock));
        }
    }

    public @NotNull Map<Long, Block> save() {
        var map = new HashMap<Long, Block>(blocks.size());
        for (var entry : blocks.long2ObjectEntrySet()) {
            if (isBlockTransient(entry.getValue())) continue;
            map.put(entry.getLongKey(), entry.getValue());
        }
        return map;
    }

    public void load(@NotNull Map<Long, Block> blocks) {
        clear();
        for (var entry : blocks.entrySet()) {
            var blockPosition = PositionUtil.unpackPosition(entry.getKey());
            setBlock(blockPosition, entry.getValue());
        }
    }

    public void submitTask(@NotNull Point blockPosition, @NotNull BlockUpdateTask task, boolean replace) {
        long blockIndex = PositionUtil.packPosition(blockPosition);
        var existing = tasks.get(blockIndex);
        if (existing != null) {
            if (!replace) return;

            tasks.remove(blockIndex);
            existing.cancel();
        }

        var wrapper = new TaskWrapper(blockPosition, task);
        wrapper.handle = player.scheduler().submitTask(wrapper);
        tasks.put(blockIndex, wrapper);
    }

    private static boolean isBlockTransient(@NotNull Block block) {
        return block.id() == Block.BIG_DRIPLEAF.id();
    }

    private class TaskWrapper implements Supplier<TaskSchedule> {
        private final Point blockPosition;
        private final BlockUpdateTask task;
        private Task handle;

        private TaskWrapper(@NotNull Point blockPosition, @NotNull BlockUpdateTask task) {
            this.blockPosition = blockPosition;
            this.task = task;
        }

        public void cancel() {
            var handle = this.handle;
            this.handle = null;
            if (handle == null) return;
            tasks.remove(PositionUtil.packPosition(blockPosition));
            setBlock(blockPosition, null);
            handle.cancel();
        }

        @Override
        public TaskSchedule get() {
            var result = task.execute(getBlock(blockPosition, Condition.TYPE));
            var schedule = result.getKey();
            if (schedule <= 0) {
                cancel();
                return TaskSchedule.stop();
            }

            setBlock(blockPosition, result.getValue());
            return TaskSchedule.tick(schedule);
        }

    }

}
