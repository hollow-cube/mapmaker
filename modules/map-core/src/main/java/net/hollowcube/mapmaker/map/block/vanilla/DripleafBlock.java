package net.hollowcube.mapmaker.map.block.vanilla;

import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.block.ghost.BlockUpdateTask;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.kyori.adventure.key.Key;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.Locale;
import java.util.Map;

public class DripleafBlock implements BlockHandler, CollidableBlock {
    private static final Key KEY = Key.key("minecraft:dripleaf");

    private static final BoundingBox COLLISION_BOX = new BoundingBox(1, 5 / 16.0, 1, new Vec(-0.5, 11 / 16.0, -0.5));

    public static final DripleafBlock INSTANCE = new DripleafBlock();

    private DripleafBlock() {
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public BoundingBox collisionBox() {
        return COLLISION_BOX;
    }

    @Override
    public void onEnter(Collision collision) {
        if (!collision.world().shouldTriggerDripleaf(collision.player()))
            return;

        GhostBlockHolder ghostBlocks = GhostBlockHolder.forPlayer(collision.player());
        ghostBlocks.submitTask(collision.blockPosition(), new DecayTask(), false);
    }

    public void handleProjectileCollision(Collision collision) {
        if (!collision.world().shouldTriggerDripleaf(collision.player())) {
            // The client predicted the hit we need to revert it to the server state
            collision.player().sendPacket(new BlockChangePacket(collision.blockPosition(), collision.block()));
            return;
        }

        // Otherwise it should immediately go to last state and reset as normal
        GhostBlockHolder ghostBlocks = GhostBlockHolder.forPlayer(collision.player());
        ghostBlocks.submitTask(collision.blockPosition(), new DecayTask(Tilt.PARTIAL), true);
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

        public Tilt next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    private static class DecayTask implements BlockUpdateTask {
        private Tilt state = Tilt.NONE;

        public DecayTask() {
            this(Tilt.NONE);
        }

        public DecayTask(Tilt initialState) {
            this.state = initialState;
        }

        @Override
        public Map.Entry<Integer, Block> execute(Block block) {
            this.state = state.next();
            if (this.state == Tilt.NONE) return BlockUpdateTask.STOP;
            return Map.entry(this.state.delay, block.withProperty("tilt", this.state.name().toLowerCase(Locale.ROOT)));
        }
    }
}
