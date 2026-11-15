package net.hollowcube.terraform.bench;

import net.hollowcube.terraform.apply.ApplyEngine;
import net.hollowcube.terraform.apply.ChangeTracker;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.GameDataProvider;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.openjdk.jmh.annotations.*;

import java.util.BitSet;
import java.util.Random;

/// [ChangeTracker] costs over an 8x8 chunk area whose sections 0 to 7 hold dense 8-state terrain (512 sections):
/// what tracking adds to a sparse apply, host marks, draining sparse and whole sections, and serializing a drain.
public class ChangeTrackerBenchmark {
    private static final int CHUNKS = 8;
    private static final int SECTIONS = 8;
    private static final int SPARSE_CELLS = 32;
    private static final int WHOLE_CELLS = 65;
    private static final Block[] TERRAIN = {
        Block.STONE,
        Block.DIRT,
        Block.GRANITE,
        Block.ANDESITE,
        Block.DIORITE,
        Block.GRAVEL,
        Block.COBBLESTONE,
        Block.DEEPSLATE,
    };

    @State(Scope.Benchmark)
    public static class World {
        Instance untracked, tracked, drained, marked;
        ChangeTracker applyTracker, drainTracker, markTracker;
        EditBuffer scatterA, scatterB, wholeDrain;
        NetworkBuffer.Type<EditBuffer> serializer;
        // x, y, z of SPARSE_CELLS cells per section, then WHOLE_CELLS per section
        int[] sparseCells, wholeCells;
        boolean flipUntracked, flipTracked;

        @Setup(Level.Trial)
        public void setup() {
            MinecraftServer.init();
            untracked = terrain();
            tracked = terrain();
            drained = terrain();
            marked = terrain();
            applyTracker = ChangeTracker.attach(tracked, null);
            drainTracker = ChangeTracker.attach(drained, null);
            markTracker = ChangeTracker.attach(marked, null);
            sparseCells = cells(SPARSE_CELLS, 1);
            wholeCells = cells(WHOLE_CELLS, 2);
            scatterA = scatter(Block.GLASS);
            scatterB = scatter(Block.GOLD_BLOCK);
            serializer = EditBuffer.serializer(
                GameDataProvider.NOOP,
                MinecraftServer.getBiomeRegistry()
            );
            mark(drainTracker, wholeCells);
            wholeDrain = drainTracker.drain();
        }

        private EditBuffer scatter(Block block) {
            var out = EditBuffer.builder();
            for (int i = 0; i < sparseCells.length; i += 3)
                out.setBlockState(sparseCells[i], sparseCells[i + 1], sparseCells[i + 2], block);
            return out.build();
        }
    }

    @State(Scope.Thread)
    public static class SparseMarks {
        @Setup(Level.Invocation)
        public void mark(World world) {
            ChangeTrackerBenchmark.mark(world.drainTracker, world.sparseCells);
        }
    }

    @State(Scope.Thread)
    public static class WholeMarks {
        @Setup(Level.Invocation)
        public void mark(World world) {
            ChangeTrackerBenchmark.mark(world.drainTracker, world.wholeCells);
        }
    }

    @Benchmark
    public ApplyEngine.ApplyResult applyScatterUntracked(World world) {
        world.flipUntracked = !world.flipUntracked;
        return ApplyEngine.DEFAULT.apply(
            world.untracked,
            world.flipUntracked ? world.scatterA : world.scatterB
        );
    }

    @Benchmark
    public ApplyEngine.ApplyResult applyScatterTracked(World world) {
        world.flipTracked = !world.flipTracked;
        return ApplyEngine.DEFAULT.apply(
            world.tracked,
            world.flipTracked ? world.scatterA : world.scatterB
        );
    }

    @Benchmark
    public ChangeTracker hostMarks(World world) {
        mark(world.markTracker, world.sparseCells);
        return world.markTracker;
    }

    @Benchmark
    public EditBuffer drainSparse(World world, SparseMarks marks) {
        return world.drainTracker.drain();
    }

    @Benchmark
    public EditBuffer drainWhole(World world, WholeMarks marks) {
        return world.drainTracker.drain();
    }

    @Benchmark
    public byte[] serializeWholeDrain(World world) {
        return NetworkBuffer.makeArray(out -> out.write(world.serializer, world.wholeDrain));
    }

    private static void mark(ChangeTracker tracker, int[] cells) {
        for (int i = 0; i < cells.length; i += 3)
            tracker.markBlock(cells[i], cells[i + 1], cells[i + 2]);
    }

    private static Instance terrain() {
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        for (int cx = 0; cx < CHUNKS; cx++)
            for (int cz = 0; cz < CHUNKS; cz++) instance.loadChunk(cx, cz).join();

        var random = new Random(42);
        var out = EditBuffer.builder();
        for (int y = 0; y < SECTIONS * 16; y++)
            for (int z = 0; z < CHUNKS * 16; z++)
                for (int x = 0; x < CHUNKS * 16; x++)
                    out.setBlockState(x, y, z, TERRAIN[random.nextInt(TERRAIN.length)]);
        ApplyEngine.DEFAULT.apply(instance, out.build());
        return instance;
    }

    /// `perSection` distinct random cells in every section, as x, y, z triples.
    private static int[] cells(int perSection, long seed) {
        var random = new Random(seed);
        int[] out = new int[CHUNKS * CHUNKS * SECTIONS * perSection * 3];
        int i = 0;
        for (int sx = 0; sx < CHUNKS; sx++)
            for (int sz = 0; sz < CHUNKS; sz++)
                for (int sy = 0; sy < SECTIONS; sy++) {
                    var picked = new BitSet(4096);
                    while (picked.cardinality() < perSection) picked.set(random.nextInt(4096));
                    for (int cell = picked.nextSetBit(0); cell >= 0; cell = picked.nextSetBit(
                        cell + 1
                    )) {
                        out[i++] = (sx << 4) + (cell & 15);
                        out[i++] = (sy << 4) + (cell >> 8);
                        out[i++] = (sz << 4) + ((cell >> 4) & 15);
                    }
                }
        return out;
    }
}
