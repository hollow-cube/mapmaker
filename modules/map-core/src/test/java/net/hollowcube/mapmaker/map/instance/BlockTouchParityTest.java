package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.entity.PhysicsTestEnv;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Plates used to be found by walking the blocks under the player and testing each collidable handler's box. They
/// are now found through the node index's tree, which must give exactly the same answer everywhere, including
/// where boxes only touch and at coordinates large enough for the tree's floats to round.
class BlockTouchParityTest {
    static final CollidableBlock PLATE = new WorldIndexTest.TestCollidable(Key.key("test:parity_plate"),
        new BoundingBox(14.0 / 16.0, 4.0 / 16.0, 14.0 / 16.0));
    // The dripleaf's box: the top 5/16 of the block.
    static final CollidableBlock LEAF = new WorldIndexTest.TestCollidable(Key.key("test:parity_leaf"),
        new BoundingBox(1, 5 / 16.0, 1, new Vec(-0.5, 11 / 16.0, -0.5)));

    static final List<BoundingBox> POSES = List.of(
        new BoundingBox(0.6, 1.8, 0.6), // standing
        new BoundingBox(0.6, 1.5, 0.6), // sneaking
        new BoundingBox(0.6, 0.6, 0.6)  // swimming, crawling
    );
    static final double[] NUDGES = {0, 1e-7, -1e-7, 1e-4, -1e-4};

    static MapInstance instance;
    static List<BlockVec> nodes;

    @BeforeAll
    static void setup() {
        PhysicsTestEnv.init();
        instance = new MapInstance("parity", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED);
        instance.loadChunk(0, 0).join();
        instance.loadChunk(1875, -1875).join();

        nodes = List.of(
            new BlockVec(2, 41, 2),
            new BlockVec(3, 41, 2),     // adjacent plates
            new BlockVec(2, 42, 3),     // a plate a block up and over
            new BlockVec(8, 41, 8),     // leaf
            new BlockVec(30005, 70, -29995)
        );
        for (var pos : nodes) {
            var handler = pos.x() == 8 ? LEAF : PLATE;
            instance.setBlock(pos, Block.STONE_PRESSURE_PLATE.withHandler(handler));
        }
        instance.index().update(instance, MapSize.NORMAL);
    }

    @AfterAll
    static void teardown() {
        MinecraftServer.getInstanceManager().unregisterInstance(instance);
    }

    @Test
    void indexMatchesBlockWalk() {
        int checked = 0, touching = 0;
        for (var node : nodes) {
            var handler = (CollidableBlock) instance.getBlock(node).handler();
            var box = handler.collisionBox();
            for (var pose : POSES) {
                for (double x : axis(node.x() + 0.5, box.minX(), box.maxX(), pose.width() / 2, pose.width() / 2)) {
                    for (double z : axis(node.z() + 0.5, box.minZ(), box.maxZ(), pose.depth() / 2, pose.depth() / 2)) {
                        for (double y : axis(node.y(), box.minY(), box.maxY(), 0, pose.height())) {
                            var position = new Pos(x, y, z);
                            var expected = blockWalk(position, pose);
                            var actual = indexed(position, pose);
                            assertEquals(expected, actual, () -> "at " + position + " with " + pose);
                            checked++;
                            if (!expected.isEmpty()) touching++;
                        }
                    }
                }
            }
        }
        assertFalse(touching == 0 || touching == checked, "sweep should cover both touching and not");
    }

    /// Positions of a player's origin along one axis around a node: a coarse grid, plus every value where the
    /// player's box exactly meets the node's box, nudged either way.
    private static Set<Double> axis(double origin, double boxMin, double boxMax, double extentBelow, double extentAbove) {
        var values = new TreeSet<Double>();
        for (double offset = -2; offset <= 2; offset += 0.25) values.add(origin + offset);
        for (double edge : new double[]{origin + boxMin - extentAbove, origin + boxMax + extentBelow}) {
            for (double nudge : NUDGES) values.add(edge + nudge);
        }
        return values;
    }

    /// The pre-index implementation from `MapPlayer#updateTouchingBlocks`.
    private static Set<BlockVec> blockWalk(Pos position, BoundingBox bb) {
        var result = new HashSet<BlockVec>();
        var iter = bb.getBlocks(position);
        while (iter.hasNext()) {
            var posMut = iter.next();
            var pos = new BlockVec(posMut.x(), posMut.y(), posMut.z());
            if (!instance.isChunkLoaded(pos)) continue;
            var block = instance.getBlock(pos);
            if (!(block.handler() instanceof CollidableBlock collidable)) continue;
            if (bb.intersectBox(position.sub(pos.blockX() + 0.5, pos.blockY(), pos.blockZ() + 0.5), collidable.collisionBox()))
                result.add(pos);
        }
        return result;
    }

    private static Set<BlockVec> indexed(Pos position, BoundingBox bb) {
        var query = new net.hollowcube.mapmaker.map.util.spatial.BoundingBox(
            (float) (position.x() + bb.minX()), (float) (position.y() + bb.minY()), (float) (position.z() + bb.minZ()),
            (float) (position.x() + bb.maxX()), (float) (position.y() + bb.maxY()), (float) (position.z() + bb.maxZ())
        );
        var result = new HashSet<BlockVec>();
        for (var object : instance.index().tree().intersectingObjects(query)) {
            if (object instanceof WorldIndex.IndexedBlock node && node.touches(position, bb)) result.add(node.position());
        }
        return result;
    }
}
