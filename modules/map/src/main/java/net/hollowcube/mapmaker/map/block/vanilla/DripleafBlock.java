package net.hollowcube.mapmaker.map.block.vanilla;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.mapmaker.map.MapHooks;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.util.PositionUtil;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

public class DripleafBlock implements BlockHandler {
    private static final NamespaceID ID = NamespaceID.from("minecraft:dripleaf");

    private static final BoundingBox BOUNDING_BOX = new BoundingBox(1, 2.0 / 16.0, 1);

    // This handler is a very weird singleton even though it does have some internal state.
    // It is safe because an entity id can only be in one place at once, but definitely weird.
    public static final DripleafBlock INSTANCE = new DripleafBlock();

    private DripleafBlock() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    public void clearPlayer(@NotNull Player player, boolean delete) {
        var manager = DripleafManager.forPlayerOptional(player);
        if (manager != null) manager.clear();
        if (delete) player.removeTag(DripleafManager.TAG);
    }

    public static @Nullable Block getDripleafState(@NotNull Player player, @NotNull Point blockPosition) {
        var manager = DripleafManager.forPlayerOptional(player);
        if (manager == null) return null;
        return manager.getDripleafState(blockPosition);
    }

    @Override
    public boolean isTickable() {
        return true;
    }

    @Override
    public void tick(@NotNull BlockHandler.Tick tick) {
        var instance = tick.getInstance();
        var world = MapWorld.unsafeFromInstance(instance);
        if (world instanceof EditingMapWorld editingWorld) {
            // This is a bit of a specific exception, probably this should be rewritten to use MapWorld.forPlayerOptional
            // on every nearby player which will return the testing world _only_ if they are in it.
            world = editingWorld.testWorld();
        }
        if (world == null) return;

        var pos = tick.getBlockPosition();
        //noinspection ConstantValue
        if (pos == null)
            return; // Intellij doesnt like this because it disagrees with annotation, but minestom seems to lie here sometimes.
        var centerPos = new Vec(pos.blockX() + 0.5, pos.blockY() + (15.0 / 16.0), pos.blockZ() + 0.5);

        for (var entity : instance.getNearbyEntities(tick.getBlockPosition(), 2)) {
            Player player;
            if (entity instanceof Player p) {
                player = p;
            } else if (entity.hasTag(MapHooks.ASSOCIATED_PLAYER)) {
                player = entity.getTag(MapHooks.ASSOCIATED_PLAYER);
            } else continue;

            if (!world.isPlaying(player)) continue;
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null || (saveState.getPlayStartTime() == 0)) continue;
            if (!BOUNDING_BOX.intersectBox(centerPos.sub(entity.getPosition()), entity.getBoundingBox()))
                continue;

            // Player just started standing on the dripleaf, so start the sequence
            DripleafManager.forPlayer(player).handleTouch(pos);
        }
    }


    private enum Tilt {
        NONE(0),
        UNSTABLE(10),
        PARTIAL(10),
        FULL(100);

        private static final Tilt[] VALUES = values();
        private final int delay;

        Tilt(int delay) {
            this.delay = delay;
        }

        public int delay() {
            return delay;
        }

        public @NotNull Tilt next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    private static class DripleafManager {
        private static final Tag<DripleafManager> TAG = Tag.Transient("dripleaf_manager");

        public static @Nullable DripleafManager forPlayerOptional(@NotNull Player player) {
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

        public static @NotNull DripleafManager forPlayer(@NotNull Player player) {
            var existing = forPlayerOptional(player);
            if (existing != null) return existing;

            var manager = new DripleafManager(player);
            player.setTag(TAG, manager);
            return manager;
        }

        private final Player player;
        private final Instance instance;

        private final Long2ObjectMap<Task> tasks = new Long2ObjectArrayMap<>();

        public DripleafManager(@NotNull Player player) {
            this.player = player;
            this.instance = player.getInstance();
        }

        public void handleTouch(@NotNull Point blockPosition) {
            long index = PositionUtil.packPosition(blockPosition);
            var task = tasks.get(index);
            if (task != null) return; // Already falling

            task = new Task(blockPosition);
            tasks.put(index, task);
            player.scheduler().submitTask(task);
        }

        public void clear() {
            tasks.values().forEach(Task::cancel);
            tasks.clear();
        }

        public @Nullable Block getDripleafState(@NotNull Point blockPosition) {
            var task = tasks.get(PositionUtil.packPosition(blockPosition));
            if (task == null) return null;
            return task.block.withProperty("tilt", task.state.name().toLowerCase(Locale.ROOT));
        }

        @SuppressWarnings("UnstableApiUsage")
        private class Task implements Supplier<TaskSchedule> {
            private final Point blockPosition;
            private final Block block;

            private Tilt state = Tilt.NONE;

            public Task(@NotNull Point blockPosition) {
                this.blockPosition = blockPosition;
                this.block = instance.getBlock(blockPosition);
            }

            public void cancel() {
                setBlockTilt(Tilt.NONE);
                state = null;
            }

            @Override
            public TaskSchedule get() {
                if (state == null || !player.getInstance().equals(instance))
                    return TaskSchedule.stop(); // Canceled or the player left the instance

                state = state.next();
                setBlockTilt(state);
                if (state == Tilt.NONE) {
                    tasks.remove(PositionUtil.packPosition(blockPosition));
                    return TaskSchedule.stop();
                }
                return TaskSchedule.tick(state.delay());
            }

            private void setBlockTilt(@NotNull Tilt tilt) {
                var blockState = block.withProperty("tilt", tilt.name().toLowerCase(Locale.ROOT)).stateId();
                player.sendPacket(new BlockChangePacket(blockPosition, blockState));
            }
        }
    }
}
