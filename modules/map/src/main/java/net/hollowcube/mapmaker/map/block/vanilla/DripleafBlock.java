package net.hollowcube.mapmaker.map.block.vanilla;

import net.hollowcube.mapmaker.map.MapHooks;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.block.ghost.BlockUpdateTask;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.kyori.adventure.key.Key;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

public class DripleafBlock implements BlockHandler {
    private static final Key ID = Key.key("minecraft:dripleaf");

    private static final BoundingBox BOUNDING_BOX = new BoundingBox(1, 2.0 / 16.0, 1);

    public static final DripleafBlock INSTANCE = new DripleafBlock();

    private DripleafBlock() {
    }

    @Override
    public @NotNull Key getKey() {
        return ID;
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
            GhostBlockHolder ghostBlocks = GhostBlockHolder.forPlayer(player);
            ghostBlocks.submitTask(pos, new DecayTask(), false);
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

    private static class DecayTask implements BlockUpdateTask {
        private Tilt state = Tilt.NONE;

        @Override
        public @NotNull Map.Entry<Integer, Block> execute(@NotNull Block block) {
            this.state = state.next();
            if (this.state == Tilt.NONE) return BlockUpdateTask.STOP;
            return Map.entry(this.state.delay, block.withProperty("tilt", this.state.name().toLowerCase(Locale.ROOT)));
        }
    }
}
