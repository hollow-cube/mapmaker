package net.hollowcube.mapmaker.map.block.vanilla;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.hollowcube.mapmaker.map.MapHooks;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Supplier;

public class DripleafBlock implements BlockHandler {
    private static final NamespaceID ID = NamespaceID.from("minecraft:dripleaf");

    // This handler is a very weird singleton even though it does have some internal state.
    // It is safe because an entity id can only be in one place at once, but definitely weird.
    public static final DripleafBlock INSTANCE = new DripleafBlock();

    // The players whose dripleaf is being tracked, mapped to the task that is tracking it.
    private final Int2ObjectMap<DripleafTask> playerTasks = Int2ObjectMaps.synchronize(new Int2ObjectArrayMap<>());

    private DripleafBlock() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    public void clearPlayer(@NotNull Player player) {
        var task = playerTasks.remove(player.getEntityId());
        if (task != null) task.cancel();
    }

    @Override
    public void onTouch(@NotNull BlockHandler.Touch touch) {
        var instance = touch.getInstance();
        var world = MapWorld.unsafeFromInstance(instance);
        if (world instanceof EditingMapWorld editingWorld) {
            // This is a bit of a specific exception, probably this should be rewritten to use MapWorld.forPlayerOptional
            // on every nearby player which will return the testing world _only_ if they are in it.
            world = editingWorld.testWorld();
        }
        if (world == null) return;

        var entity = touch.getTouching();
        Player player;
        if (entity instanceof Player p) {
            player = p;
        } else if (entity.hasTag(MapHooks.ASSOCIATED_PLAYER)) {
            player = entity.getTag(MapHooks.ASSOCIATED_PLAYER);
        } else return;

        if (playerTasks.containsKey(player.getEntityId()) || !world.isPlaying(player)) return;
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null || (saveState.getPlayStartTime() == 0)) return;

        // Ensure they are standing on top
        var blockPosition = touch.getBlockPosition();
        if (player.getPosition().y() < blockPosition.y() + 0.9375) return;

        // Player just started standing on the dripleaf, so start the sequence
        var task = new DripleafTask(player, instance, blockPosition);
        playerTasks.put(player.getEntityId(), task);
        player.scheduler().submitTask(task);
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

    @SuppressWarnings("UnstableApiUsage")
    private class DripleafTask implements Supplier<TaskSchedule> {
        private final Player player;
        private final Instance instance;
        private final Point blockPosition;
        private final Block block;

        private Tilt state = Tilt.NONE;

        public DripleafTask(@NotNull Player player, @NotNull Instance instance, @NotNull Point blockPosition) {
            this.player = player;
            this.instance = instance;
            this.blockPosition = blockPosition;
            this.block = instance.getBlock(blockPosition);
        }

        public void cancel() {
            setBlockTilt(Tilt.NONE);
            state = Tilt.NONE;
        }

        @Override
        public TaskSchedule get() {
            if (state == null || !player.getInstance().equals(instance))
                return TaskSchedule.stop(); // Canceled or the player left the instance

            state = state.next();
            setBlockTilt(state);
            if (state == Tilt.NONE) {
                playerTasks.remove(player.getEntityId());
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
