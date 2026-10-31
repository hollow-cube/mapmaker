package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.mapmaker.map.entity.PhysicsTestEnv;
import net.hollowcube.terraform.apply.ApplyEngine;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A Terraform apply, and a vanilla block change, keep mapmaker's own heightmaps, including the bottom-up
/// world-bottom map, equal to a full recalculation.
class HeightmapApplyTest {
    private static final Block[] BLOCKS = {
        Block.AIR,
        Block.STONE,
        Block.DIRT,
        Block.COBWEB,
        Block.BAMBOO_SAPLING,
        Block.WATER,
        Block.OAK_SLAB,
        Block.DANDELION,
    };
    private static final int[] MAPS = {Heightmaps.WORLD_SURFACE, Heightmaps.MOTION_BLOCKING, Heightmaps.WORLD_BOTTOM};
    private static final ApplyEngine ENGINE = ApplyEngine.builder()
        .heightmap(chunk -> ((ChunkExt) chunk).heightmaps().asTerraformViews())
        .build();

    @BeforeAll
    static void setup() {
        PhysicsTestEnv.init();
    }

    @Test
    void applyMatchesRecalculation() {
        var instance = new MapInstance("heightmaps", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED);
        try {
            Chunk chunk = instance.loadChunk(0, 0).join();
            var heightmaps = ((ChunkExt) chunk).heightmaps();

            var random = new Random(31337);
            for (int iteration = 0; iteration < 10; iteration++) {
                var edit = randomEdit(random);
                var result = apply(instance, edit);
                assertMatchesRecalculation(chunk, heightmaps);
                apply(instance, result.undo());
                assertMatchesRecalculation(chunk, heightmaps);
                apply(instance, edit);
                assertMatchesRecalculation(chunk, heightmaps);
            }
        } finally {
            MinecraftServer.getInstanceManager().unregisterInstance(instance);
        }
    }

    @Test
    void neverComputedHeightmapsAreComputedBeforeAnApply() {
        var instance = new MapInstance("heightmaps", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED);
        try {
            Chunk chunk = instance.loadChunk(0, 0).join();
            // Terrain written without touching mapmaker's heightmaps, which stay uncomputed.
            var terrain = EditBuffer.builder();
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) terrain.setBlockState(x, 10 + x, z, Block.STONE);
            }
            ApplyEngine.DEFAULT.apply(instance, terrain.build());

            apply(instance, EditBuffer.builder().setBlockState(3, 100, 3, Block.STONE).build());
            assertMatchesRecalculation(chunk, ((ChunkExt) chunk).heightmaps());
        } finally {
            MinecraftServer.getInstanceManager().unregisterInstance(instance);
        }
    }

    @Test
    void breakingNextToTheSurfaceKeepsIt() {
        var instance = new MapInstance("heightmaps", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED);
        try {
            Chunk chunk = instance.loadChunk(0, 0).join();
            instance.setBlock(1, 10, 1, Block.STONE);
            instance.setBlock(1, 9, 1, Block.STONE);
            instance.setBlock(1, 11, 1, Block.STONE);
            instance.setBlock(1, 12, 1, Block.STONE);

            instance.setBlock(1, 11, 1, Block.AIR); // just below the top
            instance.setBlock(1, 10, 1, Block.AIR); // just above the bottom

            var heightmaps = ((ChunkExt) chunk).heightmaps();
            assertEquals(12, heightmaps.get(Heightmaps.WORLD_SURFACE, 1, 1));
            assertEquals(9, heightmaps.get(Heightmaps.WORLD_BOTTOM, 1, 1));
        } finally {
            MinecraftServer.getInstanceManager().unregisterInstance(instance);
        }
    }

    private static ApplyEngine.ApplyResult apply(MapInstance instance, EditBuffer edit) {
        return ENGINE.apply(instance, edit);
    }

    private static EditBuffer randomEdit(Random random) {
        var builder = EditBuffer.builder();
        for (int cell = 0; cell < 400; cell++) {
            builder.setBlockState(random.nextInt(16), random.nextInt(-64, 128), random.nextInt(16),
                BLOCKS[random.nextInt(BLOCKS.length)]);
        }
        for (int section = 0; section < 3; section++) {
            builder.fillSection(0, random.nextInt(-4, 8), 0, BLOCKS[random.nextInt(BLOCKS.length)].stateId());
        }
        int denseY = random.nextInt(-4, 8) << 4;
        for (int y = denseY; y < denseY + 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) builder.setBlockState(x, y, z, BLOCKS[random.nextInt(BLOCKS.length)]);
            }
        }
        return builder.build();
    }

    private static void assertMatchesRecalculation(Chunk chunk, Heightmaps heightmaps) {
        var fresh = new Heightmaps(chunk);
        recalculate(chunk, fresh);
        for (int map : MAPS) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    assertEquals(fresh.get(map, x, z), heightmaps.get(map, x, z), "map " + map + " at " + x + "," + z);
                }
            }
        }
    }

    private static void recalculate(Chunk chunk, Heightmaps heightmaps) {
        chunk.lockReadLock();
        try {
            for (int map : MAPS) heightmaps.heightmap(map).recalculate();
        } finally {
            chunk.unlockReadLock();
        }
    }
}
