package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.heightmap.MotionBlockingHeightmap;
import net.minestom.server.instance.heightmap.WorldSurfaceHeightmap;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnvTest
class HeightmapChangesTest {
    private static final Block[] MIXED_BLOCKS = {
        Block.AIR,
        Block.CAVE_AIR,
        Block.STONE,
        Block.DIRT,
        Block.DANDELION,
        Block.COBWEB,
        Block.BAMBOO_SAPLING,
        Block.WATER,
        Block.LAVA,
        Block.OAK_LEAVES,
        Block.OAK_SLAB.withProperty("waterlogged", "true"),
        Block.OAK_SLAB,
    };

    @Test
    void ignoresNoOpsNbtEquivalentPredicatesAndBuriedChanges(Env env) {
        var chunk = chunk(env);
        var heightmaps = new CountingMinestom();
        apply(
            chunk,
            heightmaps,
            EditBuffer.builder()
                .setBlockState(1, 90, 1, Block.STONE)
                .setBlockState(2, 90, 2, Block.STONE)
                .setBlockState(3, 90, 3, Block.STONE)
                .build()
        );
        heightmaps.reset();

        apply(
            chunk,
            heightmaps,
            EditBuffer.builder()
                .setBlockState(1, 90, 1, Block.STONE)
                .setBlockState(1, 70, 1, Block.STONE)
                .setBlockState(2, 90, 2, Block.DIRT)
                .setBlockState(
                    3,
                    90,
                    3,
                    Block.STONE.withNbt(CompoundBinaryTag.builder().putInt("test", 1).build())
                )
                .setBlockState(4, 90, 4, Block.STONE)
                .build()
        );

        assertEquals(1, heightmaps.motionWrites);
        assertEquals(1, heightmaps.surfaceWrites);
        assertMatchesFreshHeightmaps(chunk);
    }

    @Test
    void tracksTheTwoPredicatesIndependently(Env env) {
        var chunk = chunk(env);
        var heightmaps = new CountingMinestom();
        apply(
            chunk,
            heightmaps,
            EditBuffer.builder().setBlockState(1, 90, 1, Block.DANDELION).build()
        );
        heightmaps.reset();
        apply(chunk, heightmaps, EditBuffer.builder().setBlockState(1, 90, 1, Block.WATER).build());
        assertEquals(1, heightmaps.motionWrites);
        assertEquals(0, heightmaps.surfaceWrites);
        assertMatchesFreshHeightmaps(chunk);

        heightmaps.reset();
        apply(
            chunk,
            heightmaps,
            EditBuffer.builder().setBlockState(1, 90, 1, Block.DANDELION).build()
        );
        assertEquals(1, heightmaps.motionWrites);
        assertEquals(0, heightmaps.surfaceWrites);
        assertMatchesFreshHeightmaps(chunk);
    }

    @Test
    void fullReplacementOnlyWritesChangedColumnTops(Env env) {
        var chunk = chunk(env);
        var heightmaps = new CountingMinestom();
        apply(
            chunk,
            heightmaps,
            EditBuffer.builder().fillSection(0, 4, 0, Block.STONE.stateId()).build()
        );
        heightmaps.reset();
        var replace = EditBuffer.builder();
        for (int y = 64; y < 80; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    replace.setBlockState(x, y, z, x == 3 && z == 5 ? Block.AIR : Block.DIRT);
                }
            }
        }
        apply(chunk, heightmaps, replace.build());
        assertEquals(1, heightmaps.motionWrites);
        assertEquals(1, heightmaps.surfaceWrites);
        assertMatchesFreshHeightmaps(chunk);
    }

    @Test
    void uniformReplacementsSkipEquivalentAndCoveredSections(Env env) {
        var chunk = chunk(env);
        var heightmaps = new CountingMinestom();
        apply(
            chunk,
            heightmaps,
            EditBuffer.builder().fillSection(0, 4, 0, Block.STONE.stateId()).build()
        );
        heightmaps.reset();
        apply(
            chunk,
            heightmaps,
            EditBuffer.builder()
                .fillSection(0, 4, 0, Block.DIRT.stateId())
                .fillSection(0, 3, 0, Block.AIR.stateId())
                .build()
        );
        assertEquals(0, heightmaps.motionWrites);
        assertEquals(0, heightmaps.surfaceWrites);
        assertMatchesFreshHeightmaps(chunk);

        var result = apply(
            chunk,
            heightmaps,
            EditBuffer.builder()
                .fillSection(0, 4, 0, Block.DANDELION.stateId())
                .fillSection(0, 2, 0, Block.WATER.stateId())
                .build()
        );
        assertMatchesFreshHeightmaps(chunk);
        apply(chunk, heightmaps, result.undo());
        assertMatchesFreshHeightmaps(chunk);
    }

    @Test
    void mixedEditsUndoAndRedoMatchFullRefresh(Env env) {
        var chunk = chunk(env);
        var random = new Random(94721);
        for (int iteration = 0; iteration < 12; iteration++) {
            var edit = mixedEdit(random);
            var result = apply(chunk, HeightmapView.MINESTOM, edit);
            assertMatchesFreshHeightmaps(chunk);
            apply(chunk, HeightmapView.MINESTOM, result.undo());
            assertMatchesFreshHeightmaps(chunk);
            apply(chunk, HeightmapView.MINESTOM, edit);
            assertMatchesFreshHeightmaps(chunk);
        }
    }

    @Test
    void hostHeightmapsInBothDirectionsMatchFullRecalculation(Env env) {
        var chunk = chunk(env);
        IntPredicate notAir = state -> !Block.fromStateId(state).air();
        IntPredicate solid = state -> Block.fromStateId(state).solid();
        var maps = List.of(
            new ArrayHeightmap(chunk, false, notAir),
            new ArrayHeightmap(chunk, false, solid),
            new ArrayHeightmap(chunk, true, notAir),
            new ArrayHeightmap(chunk, true, solid)
        );
        Function<Chunk, Collection<HeightmapView>> provider = _ -> List.copyOf(maps);

        var random = new Random(52018);
        for (int iteration = 0; iteration < 12; iteration++) {
            var edit = mixedEdit(random);
            var result = apply(chunk, provider, edit);
            for (var map : maps) map.assertMatchesRecalculation();
            apply(chunk, provider, result.undo());
            for (var map : maps) map.assertMatchesRecalculation();
            apply(chunk, provider, edit);
            for (var map : maps) map.assertMatchesRecalculation();
        }
    }

    @Test
    void bottomUpMapFollowsTheLowestBlock(Env env) {
        var chunk = chunk(env);
        apply(
            chunk,
            HeightmapView.MINESTOM,
            EditBuffer.builder().fillSection(0, -4, 0, Block.AIR.stateId()).build()
        );
        var bottom = new ArrayHeightmap(chunk, true, state -> !Block.fromStateId(state).air());
        Function<Chunk, Collection<HeightmapView>> provider = _ -> List.of(bottom);
        int floor = bottom.height(1, 1);

        apply(chunk, provider, EditBuffer.builder().setBlockState(1, -60, 1, Block.STONE).build());
        assertEquals(-60, bottom.height(1, 1));
        apply(chunk, provider, EditBuffer.builder().setBlockState(1, -60, 1, Block.AIR).build());
        assertEquals(floor, bottom.height(1, 1));
        bottom.assertMatchesRecalculation();
    }

    private static Chunk chunk(Env env) {
        var instance = env.createFlatInstance();
        return instance.loadChunk(0, 0).join();
    }

    private static ApplyEngine.ApplyResult apply(
        Chunk chunk,
        Function<Chunk, Collection<HeightmapView>> heightmaps,
        EditBuffer buffer
    ) {
        return ApplyEngine.builder()
            .heightmap(heightmaps)
            .build()
            .apply(chunk.getInstance(), buffer);
    }

    private static EditBuffer mixedEdit(Random random) {
        var builder = EditBuffer.builder();
        for (int cell = 0; cell < 500; cell++)
            builder.setBlockState(
                random.nextInt(16), random.nextInt(-64, 128), random.nextInt(16),
                MIXED_BLOCKS[random.nextInt(MIXED_BLOCKS.length)]
            );
        for (int section = 0; section < 3; section++)
            builder.fillSection(
                0,
                random.nextInt(-4, 8),
                0,
                MIXED_BLOCKS[random.nextInt(MIXED_BLOCKS.length)].stateId()
            );
        int denseY = random.nextInt(-4, 8) << 4;
        for (int y = denseY; y < denseY + 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    builder.setBlockState(
                        x,
                        y,
                        z,
                        MIXED_BLOCKS[random.nextInt(MIXED_BLOCKS.length)]
                    );
                }
            }
        }
        return builder.build();
    }

    private static void assertMatchesFreshHeightmaps(Chunk chunk) {
        var motion = new MotionBlockingHeightmap(chunk);
        var surface = new WorldSurfaceHeightmap(chunk);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                assertEquals(
                    motion.getHeight(x, z),
                    chunk.motionBlockingHeightmap().getHeight(x, z),
                    "motion at " + x + "," + z
                );
                assertEquals(
                    surface.getHeight(x, z),
                    chunk.worldSurfaceHeightmap().getHeight(x, z),
                    "surface at " + x + "," + z
                );
            }
        }
    }

    /// The Minestom heightmaps, counting the engine's height writes to each.
    private static final class CountingMinestom implements
        Function<Chunk, Collection<HeightmapView>> {
        int motionWrites;
        int surfaceWrites;

        @Override
        public Collection<HeightmapView> apply(Chunk chunk) {
            var views = List.copyOf(HeightmapView.MINESTOM.apply(chunk));
            return List.of(
                counting(views.get(0), () -> motionWrites++),
                counting(views.get(1), () -> surfaceWrites++)
            );
        }

        void reset() {
            motionWrites = 0;
            surfaceWrites = 0;
        }

        private static HeightmapView counting(HeightmapView view, Runnable onWrite) {
            return new HeightmapView() {
                @Override
                public boolean bottomUp() {
                    return view.bottomUp();
                }

                @Override
                public boolean matches(int stateId) {
                    return view.matches(stateId);
                }

                @Override
                public int height(int x, int z) {
                    return view.height(x, z);
                }

                @Override
                public void setHeight(int x, int z, int y) {
                    onWrite.run();
                    view.setHeight(x, z, y);
                }
            };
        }
    }

    /// A host heightmap in either direction, checked against a full column scan.
    private static final class ArrayHeightmap implements HeightmapView {
        private final Chunk chunk;
        private final boolean bottomUp;
        private final IntPredicate predicate;
        private final int[] heights = new int[256];

        ArrayHeightmap(Chunk chunk, boolean bottomUp, IntPredicate predicate) {
            this.chunk = chunk;
            this.bottomUp = bottomUp;
            this.predicate = predicate;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) heights[z << 4 | x] = recalculate(x, z);
            }
        }

        @Override
        public boolean bottomUp() {
            return bottomUp;
        }

        @Override
        public boolean matches(int stateId) {
            return predicate.test(stateId);
        }

        @Override
        public int height(int x, int z) {
            return heights[z << 4 | x];
        }

        @Override
        public void setHeight(int x, int z, int y) {
            heights[z << 4 | x] = y;
        }

        void assertMatchesRecalculation() {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    assertEquals(
                        recalculate(x, z),
                        height(x, z),
                        (bottomUp ? "bottom" : "top") + " at " + x + "," + z
                    );
                }
            }
        }

        private int recalculate(int x, int z) {
            int minY = chunk.getMinSection() << 4, maxY = chunk.getMaxSection() << 4;
            chunk.lockReadLock();
            try {
                for (int i = 0; i < maxY - minY; i++) {
                    int y = bottomUp ? minY + i : maxY - 1 - i;
                    if (predicate.test(chunk.getBlock(x, y, z).stateId())) return y;
                }
                return minY - 1;
            } finally {
                chunk.unlockReadLock();
            }
        }
    }
}
