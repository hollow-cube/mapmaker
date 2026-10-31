package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.heightmap.Heightmap;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.biome.Biome;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.hollowcube.terraform.util.BlockUtil.*;
import static net.minestom.server.coordinate.CoordConversion.chunkBlockIndex;
import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class ApplyEngineTest {

    @Test
    void appliesPlainBlocks(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(1, 60, 1, Block.STONE)
            .setBlockState(2, 61, 2, Block.DIRT)
            .build();

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, buffer);

        assertEquals(Block.STONE, instance.getBlock(1, 60, 1));
        assertEquals(Block.DIRT, instance.getBlock(2, 61, 2));
        assertEquals(2, result.changed());
        assertEquals(List.of(instance.getChunk(0, 0)), result.chunks());
    }

    @Test
    void groupOrdersColumnsByZThenX(Env env) {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(17, 60, 17, Block.STONE)
            .setBlockState(0, 60, 0, Block.STONE)
            .setBlockState(-15, 60, 17, Block.STONE)
            .setBlockState(33, 60, 0, Block.STONE)
            .setBlockState(0, 60, -15, Block.STONE)
            .build();

        var order = ApplyEngine
            .prepareChunkWork(env.createFlatInstance(), buffer, UnloadedChunkPolicy.LOAD)
            .stream()
            .map(work -> List.of(work.chunk().getChunkX(), work.chunk().getChunkZ()))
            .toList();

        assertEquals(
            List.of(List.of(0, -1), List.of(0, 0), List.of(2, 0), List.of(-1, 1), List.of(1, 1)),
            order
        );
    }

    @Test
    void undoRestoresOriginal(Env env) {
        Instance instance = TestInstances.flat(env, 0, 1, 0, 0);
        int[][] points = {{1, 60, 1}, {2, 61, 2}, {17, 60, 1}, {0, 5, 0}};
        Map<Long, Block> original = new HashMap<>();
        for (int[] p : points)
            original.put(packBlockPos(p[0], p[1], p[2]), instance.getBlock(p[0], p[1], p[2]));

        EditBuffer edit = EditBuffer.builder()
            .setBlockState(1, 60, 1, Block.STONE)
            .setBlockState(2, 61, 2, Block.GLASS)
            .setBlockState(17, 60, 1, Block.DIRT) // neighbouring chunk
            .setBlockState(0, 5, 0, Block.GOLD_BLOCK) // overwrites a generated (non-air) block
            .build();
        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, edit);
        assertEquals(4, result.changed());

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        for (int[] p : points)
            assertEquals(
                original.get(packBlockPos(p[0], p[1], p[2])), instance.getBlock(p[0], p[1], p[2]),
                "restored at " + p[0] + "," + p[1] + "," + p[2]
            );
    }

    @Test
    void blockEntityRoundTrips(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        Block original = instance.getBlock(1, 60, 1);
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("Text", "hi").build();

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.OAK_SIGN.withNbt(nbt)).build()
        );

        Block placed = instance.getBlock(1, 60, 1);
        assertEquals(Block.OAK_SIGN.stateId(), placed.stateId());
        assertEquals(nbt, placed.nbt());

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(original, instance.getBlock(1, 60, 1));
    }

    @Test
    void overwritingBlockEntityWithPlainClearsNbt(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("Text", "x").build();
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.OAK_SIGN.withNbt(nbt)).build()
        );
        assertTrue(instance.getBlock(1, 60, 1).hasNbt());

        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.STONE).build()
        );

        Block after = instance.getBlock(1, 60, 1);
        assertEquals(Block.STONE, after);
        assertFalse(after.hasNbt(), "stale block entity should be wiped");
    }

    @Test
    void sameStateDifferentNbtIsApplied(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        CompoundBinaryTag nbt1 = CompoundBinaryTag.builder().putString("Text", "a").build();
        CompoundBinaryTag nbt2 = CompoundBinaryTag.builder().putString("Text", "b").build();
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.OAK_SIGN.withNbt(nbt1)).build()
        );

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.OAK_SIGN.withNbt(nbt2)).build()
        );

        assertEquals(1, result.changed(), "same state id but different NBT is still a change");
        assertEquals(nbt2, instance.getBlock(1, 60, 1).nbt());
    }

    @Test
    void noOpIsNotCounted(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.STONE).build()
        );

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.STONE).build()
        );

        assertEquals(0, result.changed());
    }

    @Test
    void crossesChunksAndSections(Env env) {
        Instance instance = TestInstances.flat(env);
        int[][] points = {{1, 60, 1}, {20, 80, 3}, {5, 100, 20}, {18, 40, 19}};
        Block[] blocks = {Block.STONE, Block.GLASS, Block.DIRT, Block.GOLD_BLOCK};

        Map<Long, Block> original = new HashMap<>();
        EditBuffer.Builder edit = EditBuffer.builder();
        for (int i = 0; i < points.length; i++) {
            int[] p = points[i];
            original.put(packBlockPos(p[0], p[1], p[2]), instance.getBlock(p[0], p[1], p[2]));
            edit.setBlockState(p[0], p[1], p[2], blocks[i]);
        }

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, edit.build());
        assertEquals(4, result.changed());
        for (int i = 0; i < points.length; i++) {
            int[] p = points[i];
            assertEquals(blocks[i], instance.getBlock(p[0], p[1], p[2]));
        }

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        for (int[] p : points)
            assertEquals(
                original.get(packBlockPos(p[0], p[1], p[2])),
                instance.getBlock(p[0], p[1], p[2])
            );
    }

    @Test
    void fuzzRoundTrip(Env env) {
        // Primary correctness harness: a random mixed edit (plain blocks incl. air, block entities
        // with NBT, a whole uniform 16³ section fill, biome cells) over several deterministic seeds,
        // asserting both legs, apply+undo restores the original world, and re-applying the edit
        // buffer restores the post-apply state. (ApplyResult exposes no separate redo buffer. The
        // pipeline's redo buffer *is* the applied buffer, so re-applying it is the faithful redo.)
        long[] seeds = {20260616L, 7L, 424242L, 987654321L};
        for (long seed : seeds) fuzzRoundTrip(env, seed);
    }

    private static void fuzzRoundTrip(Env env, long seed) {
        Instance instance = TestInstances.flat(env);
        Random rng = new Random(seed);
        Block[] palette = {
            Block.STONE,
            Block.DIRT,
            Block.GLASS,
            Block.OAK_PLANKS,
            Block.AIR,
            Block.GOLD_BLOCK,
        };

        Map<Long, Block> originalBlocks = new HashMap<>();
        Map<Long, RegistryKey<Biome>> originalBiomes = new HashMap<>();
        EditBuffer.Builder edit = EditBuffer.builder();

        // Scattered plain blocks, air included.
        for (int i = 0; i < 400; i++) {
            int x = rng.nextInt(32), y = 50 + rng.nextInt(40), z = rng.nextInt(32);
            originalBlocks.putIfAbsent(packBlockPos(x, y, z), instance.getBlock(x, y, z));
            edit.setBlockState(x, y, z, palette[rng.nextInt(palette.length)]);
        }

        // Scattered block-entity blocks carrying NBT.
        for (int i = 0; i < 40; i++) {
            int x = rng.nextInt(32), y = 50 + rng.nextInt(40), z = rng.nextInt(32);
            originalBlocks.putIfAbsent(packBlockPos(x, y, z), instance.getBlock(x, y, z));
            CompoundBinaryTag nbt = CompoundBinaryTag.builder()
                .putString("Text", "be-" + seed + "-" + i)
                .build();
            edit.setBlockState(x, y, z, (i % 2 == 0 ? Block.OAK_SIGN : Block.CHEST).withNbt(nbt));
        }

        // One whole uniform 16³ section (fillSection replaces any scattered cells already staged in
        // it) → exercises the full-cover fill fast path on apply.
        int sectionX = rng.nextInt(2), sectionY = 4 + rng.nextInt(2), sectionZ = rng.nextInt(2);
        Block fill = palette[rng.nextInt(palette.length)];
        for (int x = 0; x < 16; x++)
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++) {
                    int ax = sectionX * 16 + x, ay = sectionY * 16 + y, az = sectionZ * 16 + z;
                    originalBlocks.putIfAbsent(
                        packBlockPos(ax, ay, az),
                        instance.getBlock(ax, ay, az)
                    );
                }
        edit.fillSection(sectionX, sectionY, sectionZ, fill.stateId());

        // Biome cells, 4³-aligned so each key names a whole biome cell.
        var registry = MinecraftServer.getBiomeRegistry();
        int targetBiomeId = registry.getId(instance.getBiome(0, 64, 0)) == 0 ? 1 : 0;
        RegistryKey<Biome> targetBiome = registry.getKey(targetBiomeId);
        assertNotNull(targetBiome);
        Long probeBiomeCell = null;
        for (int i = 0; i < 24; i++) {
            int x = rng.nextInt(8) * 4, y = 52 + rng.nextInt(8) * 4, z = rng.nextInt(8) * 4;
            originalBiomes.putIfAbsent(packBlockPos(x, y, z), instance.getBiome(x, y, z));
            edit.setBiome(x, y, z, targetBiomeId);
            if (probeBiomeCell == null) probeBiomeCell = packBlockPos(x, y, z);
        }

        EditBuffer buffer = edit.build();
        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, buffer);
        assertTrue(result.changed() > 0, "seed " + seed + ": the edit changed something");

        // Snapshot the post-apply state for the redo leg.
        Map<Long, Block> appliedBlocks = new HashMap<>();
        for (long k : originalBlocks.keySet()) {
            int px = blockPosGetX(k), py = blockPosGetY(k), pz = blockPosGetZ(k);
            appliedBlocks.put(k, instance.getBlock(px, py, pz));
        }
        Map<Long, RegistryKey<Biome>> appliedBiomes = new HashMap<>();
        for (long k : originalBiomes.keySet()) {
            int px = blockPosGetX(k), py = blockPosGetY(k), pz = blockPosGetZ(k);
            appliedBiomes.put(k, instance.getBiome(px, py, pz));
        }
        assertEquals(
            targetBiome,
            appliedBiomes.get(probeBiomeCell),
            "seed " + seed + ": biome edit applied"
        );

        // Undo leg: blocks (incl. NBT/handlers) and biomes equal the original world.
        ApplyEngine.DEFAULT.apply(instance, result.undo());
        for (Map.Entry<Long, Block> e : originalBlocks.entrySet()) {
            long k = e.getKey();
            int px = blockPosGetX(k), py = blockPosGetY(k), pz = blockPosGetZ(k);
            assertEquals(
                e.getValue(),
                instance.getBlock(px, py, pz),
                "seed " + seed + ": restored at " + px + "," + py + "," + pz
            );
        }
        for (Map.Entry<Long, RegistryKey<Biome>> e : originalBiomes.entrySet()) {
            long k = e.getKey();
            int px = blockPosGetX(k), py = blockPosGetY(k), pz = blockPosGetZ(k);
            assertEquals(
                e.getValue(),
                instance.getBiome(px, py, pz),
                "seed " + seed + ": biome restored at " + px + "," + py + "," + pz
            );
        }

        // Redo leg: re-applying the edit buffer restores the post-apply state exactly.
        ApplyEngine.DEFAULT.apply(instance, buffer);
        for (Map.Entry<Long, Block> e : appliedBlocks.entrySet()) {
            long k = e.getKey();
            int px = blockPosGetX(k), py = blockPosGetY(k), pz = blockPosGetZ(k);
            assertEquals(
                e.getValue(),
                instance.getBlock(px, py, pz),
                "seed " + seed + ": redone at " + px + "," + py + "," + pz
            );
        }
        for (Map.Entry<Long, RegistryKey<Biome>> e : appliedBiomes.entrySet()) {
            long k = e.getKey();
            int px = blockPosGetX(k), py = blockPosGetY(k), pz = blockPosGetZ(k);
            assertEquals(
                e.getValue(),
                instance.getBiome(px, py, pz),
                "seed " + seed + ": biome redone at " + px + "," + py + "," + pz
            );
        }
    }

    @Test
    void fullSectionFillRoundTrips(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        EditBuffer.Builder builder = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++)
                    builder.setBlockState(
                        x,
                        y,
                        z,
                        Block.STONE
                    ); // one full section, uniform → fill fast path

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, builder.build());
        assertEquals(4096, result.changed());
        assertEquals(Block.STONE, instance.getBlock(0, 64, 0));
        assertEquals(Block.STONE, instance.getBlock(15, 79, 15));
        assertEquals(Block.STONE, instance.getBlock(8, 70, 8));

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(Block.AIR, instance.getBlock(0, 64, 0));
        assertEquals(Block.AIR, instance.getBlock(15, 79, 15));
        assertEquals(Block.AIR, instance.getBlock(8, 70, 8));
    }

    @Test
    void fillWipesAndUndoRestoresBlockEntity(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("Text", "keep").build();
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(5, 70, 5, Block.OAK_SIGN.withNbt(nbt)).build()
        );
        assertTrue(instance.getBlock(5, 70, 5).hasNbt());

        EditBuffer.Builder fill = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++) fill.setBlockState(x, y, z, Block.STONE);
        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, fill.build());

        Block filled = instance.getBlock(5, 70, 5);
        assertEquals(Block.STONE, filled);
        assertFalse(filled.hasNbt(), "fill must wipe the block entity");

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        Block restored = instance.getBlock(5, 70, 5);
        assertEquals(Block.OAK_SIGN.stateId(), restored.stateId());
        assertEquals(nbt, restored.nbt());
        assertEquals(Block.AIR, instance.getBlock(6, 70, 5), "non-entity cell restored to air");
    }

    @Test
    void entriesOnlyUniformFillCountsAndUndoes(Env env) {
        // A fill whose state already covers the section, where the only change is wiping a same-state
        // block-entity entry: it must still report changed cells (so the chunk is invalidated) and
        // capture the wiped entry into undo.
        Instance instance = TestInstances.flatChunk(env);
        EditBuffer.Builder base = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++) base.setBlockState(x, y, z, Block.STONE);
        ApplyEngine.DEFAULT.apply(instance, base.build());

        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("marker", "keep").build();
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(5, 70, 5, Block.STONE.withNbt(nbt)).build()
        );
        assertTrue(instance.getBlock(5, 70, 5).hasNbt());

        EditBuffer.Builder refill = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++) refill.setBlockState(x, y, z, Block.STONE);
        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, refill.build());

        assertEquals(1, result.changed(), "the wiped same-state entry is the one real change");
        assertFalse(instance.getBlock(5, 70, 5).hasNbt(), "fill wipes the entry");

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(nbt, instance.getBlock(5, 70, 5).nbt(), "undo restores the wiped entry");
    }

    @Test
    void fullSectionDenseRoundTrips(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        Block[] palette = {
            Block.STONE,
            Block.DIRT,
            Block.GLASS,
            Block.OAK_PLANKS,
            Block.COBBLESTONE,
        };
        Random rng = new Random(7);

        EditBuffer.Builder builder = EditBuffer.builder();
        Map<Long, Block> expected = new HashMap<>();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++) {
                    Block b = palette[rng.nextInt(palette.length)];
                    builder.setBlockState(x, y, z, b); // full-cover, non-uniform → copyFrom path
                    expected.put(packBlockPos(x, y, z), b);
                }

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, builder.build());
        assertEquals(4096, result.changed());
        for (Map.Entry<Long, Block> e : expected.entrySet()) {
            long k = e.getKey();
            int px = blockPosGetX(k), py = blockPosGetY(k), pz = blockPosGetZ(k);
            assertEquals(e.getValue(), instance.getBlock(px, py, pz));
        }

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(Block.AIR, instance.getBlock(0, 64, 0));
        assertEquals(Block.AIR, instance.getBlock(15, 79, 15));
        assertEquals(Block.AIR, instance.getBlock(8, 70, 8));
    }

    @Test
    void fullSectionDenseCarriesBlockEntity(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("Text", "in-fill").build();

        EditBuffer.Builder builder = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++)
                    builder.setBlockState(x, y, z, (x + y + z) % 2 == 0 ? Block.STONE : Block.DIRT);
        builder.setBlockState(
            3,
            68,
            9,
            Block.OAK_SIGN.withNbt(nbt)
        ); // a block entity inside the dense section

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(instance, builder.build());
        Block sign = instance.getBlock(3, 68, 9);
        assertEquals(Block.OAK_SIGN.stateId(), sign.stateId());
        assertEquals(nbt, sign.nbt());

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(Block.AIR, instance.getBlock(3, 68, 9));
    }

    @Test
    void fullSectionDenseCountsOnlyChangedCells(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        EditBuffer.Builder stone = EditBuffer.builder();
        for (int y = 64; y < 80; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) stone.setBlockState(x, y, z, Block.STONE);
            }
        }
        ApplyEngine.DEFAULT.apply(instance, stone.build());

        EditBuffer.Builder checker = EditBuffer.builder();
        for (int y = 64; y < 80; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    checker.setBlockState(x, y, z, (x + y + z) % 2 == 0 ? Block.STONE : Block.DIRT);
                }
            }
        }

        assertEquals(2048, ApplyEngine.DEFAULT.apply(instance, checker.build()).changed());
    }

    @Test
    void repeatedDenseFullCoverIsNoOp(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("Text", "same").build();
        EditBuffer.Builder builder = EditBuffer.builder();
        for (int y = 64; y < 80; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    builder.setBlockState(x, y, z, (x + y + z) % 2 == 0 ? Block.STONE : Block.DIRT);
                }
            }
        }
        builder.setBlockState(3, 68, 9, Block.OAK_SIGN.withNbt(nbt));
        EditBuffer buffer = builder.build();

        ApplyEngine.DEFAULT.apply(instance, buffer);
        ApplyEngine.ApplyResult second = ApplyEngine.DEFAULT.apply(instance, buffer);

        assertEquals(0, second.changed());
        assertTrue(second.chunks().isEmpty(), "an identical re-apply leaves the chunk clean");
    }

    @Test
    void nonDynamicChunkTargetThrowsBeforeAnyWrite(Env env) {
        var instance = (InstanceContainer) TestInstances.flatChunk(env);
        Block original = instance.getBlock(1, 60, 1);
        instance.setChunkSupplier(EmptyChunk::new); // like mapmaker's outside-the-border chunk
        instance.loadChunk(5, 5).join();
        EditBuffer edit = EditBuffer.builder()
            .setBlockState(80, 60, 80, Block.STONE)
            .setBlockState(1, 60, 1, Block.DIRT)
            .build();

        assertThrows(
            IllegalStateException.class,
            () -> ApplyEngine.DEFAULT.apply(instance, edit, UnloadedChunkPolicy.LOAD)
        );
        assertEquals(original, instance.getBlock(1, 60, 1), "nothing is written");
    }

    @Test
    void withoutUndoTheEditStillAppliesInFull(Env env) {
        Instance captured = TestInstances.flat(env);
        Instance skipped = TestInstances.flat(env);
        var builder = EditBuffer.builder()
            .fillSection(0, 2, 0, Block.GLASS.stateId())
            .setBlockState(20, 41, 3, Block.OAK_PLANKS)
            .setBlockState(21, 41, 3, Block.CHEST);
        for (int z = 0; z < 16; z++)
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++)
                    builder.setBlockState(
                        16 + x,
                        16 + y,
                        z,
                        (x + y + z) % 2 == 0 ? Block.STONE : Block.DIRT
                    );
        EditBuffer edit = builder.build();

        var withUndo = ApplyEngine.DEFAULT.apply(captured, edit, UnloadedChunkPolicy.FAIL, true);
        var withoutUndo = ApplyEngine.DEFAULT.apply(skipped, edit, UnloadedChunkPolicy.FAIL, false);

        assertEquals(0, withoutUndo.undo().sectionCount());
        assertEquals(withUndo.changed(), withoutUndo.changed());
        assertEquals(withUndo.chunks().size(), withoutUndo.chunks().size());
        for (int y = 0; y < 48; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 32; x++)
                    assertEquals(
                        captured.getBlock(x, y, z),
                        skipped.getBlock(x, y, z),
                        x + "," + y + "," + z
                    );
    }

    @Test
    void failureMidApplyCarriesTheUndoOfWhatWasWritten(Env env) {
        Instance instance = TestInstances.flat(env);
        Block original = instance.getBlock(1, 60, 1);
        Block originalInFailed = instance.getBlock(17, 60, 1);
        var engine = ApplyEngine.builder()
            .heightmap(chunk -> {
                if (chunk.getChunkX() == 1)
                    throw new IllegalStateException("broken host heightmaps");
                return HeightmapView.MINESTOM.apply(chunk);
            })
            .build();

        var failure = assertThrows(
            PartialApplyException.class,
            () -> engine.apply(
                instance,
                EditBuffer.builder()
                    .setBlockState(1, 60, 1, Block.STONE)
                    .setBlockState(17, 60, 1, Block.STONE)
                    .build()
            )
        );

        ApplyEngine.ApplyResult partial = failure.partial();
        // Heightmaps update after a column's writes, so the throwing column is written too.
        assertEquals(Block.STONE, instance.getBlock(1, 60, 1));
        assertEquals(Block.STONE, instance.getBlock(17, 60, 1));
        assertEquals(2, partial.changed());
        assertEquals(List.of(instance.getChunk(0, 0), instance.getChunk(1, 0)), partial.chunks());
        ApplyEngine.DEFAULT.apply(instance, partial.undo());
        assertEquals(original, instance.getBlock(1, 60, 1));
        assertEquals(originalInFailed, instance.getBlock(17, 60, 1));
    }

    @Test
    void repeatedFillIsNoOp(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        EditBuffer.Builder fill = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++) fill.setBlockState(x, y, z, Block.STONE);
        EditBuffer buffer = fill.build();

        ApplyEngine.DEFAULT.apply(instance, buffer);
        ApplyEngine.ApplyResult second = ApplyEngine.DEFAULT.apply(instance, buffer);
        assertEquals(0, second.changed(), "filling an already-filled section changes nothing");
    }

    @Test
    void biomeRoundTrips(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var registry = MinecraftServer.getBiomeRegistry();
        RegistryKey<Biome> origKey = instance.getBiome(0, 64, 0);
        int targetId = registry.getId(origKey) == 0 ? 1 : 0;
        RegistryKey<Biome> targetKey = registry.getKey(targetId);
        assertNotNull(targetKey);

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBiome(0, 64, 0, targetId).build()
        );

        assertEquals(targetKey, instance.getBiome(0, 64, 0));
        assertEquals(targetKey, instance.getBiome(3, 67, 3)); // same 4³ biome cell
        assertEquals(origKey, instance.getBiome(4, 64, 0)); // adjacent cell unchanged

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(origKey, instance.getBiome(0, 64, 0));
    }

    @Test
    void blocksAndBiomesApplyTogether(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var registry = MinecraftServer.getBiomeRegistry();
        RegistryKey<Biome> origBiome = instance.getBiome(2, 64, 2);
        int targetId = registry.getId(origBiome) == 0 ? 1 : 0;
        RegistryKey<Biome> targetBiome = registry.getKey(targetId);
        Block origBlock = instance.getBlock(2, 64, 2);

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder()
                .setBlockState(2, 64, 2, Block.STONE)
                .setBiome(2, 64, 2, targetId)
                .build()
        );

        assertEquals(Block.STONE, instance.getBlock(2, 64, 2));
        assertEquals(targetBiome, instance.getBiome(2, 64, 2));

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertEquals(origBlock, instance.getBlock(2, 64, 2));
        assertEquals(origBiome, instance.getBiome(2, 64, 2));
    }

    @Test
    void heightmapTracksTopBlock(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var heightmap = instance.getChunk(0, 0).motionBlockingHeightmap();
        int initial = heightmap.getHeight(5, 5); // flat terrain top

        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(5, 70, 5, Block.STONE).build()
        );
        assertEquals(70, heightmap.getHeight(5, 5), "height rises to the placed block");

        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(5, 80, 5, Block.STONE).build()
        );
        assertEquals(80, heightmap.getHeight(5, 5), "height rises to a higher placed block");

        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder()
                .setBlockState(5, 80, 5, Block.AIR)
                .setBlockState(5, 70, 5, Block.AIR)
                .build()
        );
        assertEquals(
            initial,
            heightmap.getHeight(5, 5),
            "height falls back to terrain after removal"
        );
    }

    @Test
    void heightmapCorrectAfterFill(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var heightmap = instance.getChunk(0, 0).motionBlockingHeightmap();
        EditBuffer.Builder fill = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++) fill.setBlockState(x, y, z, Block.STONE);
        ApplyEngine.DEFAULT.apply(instance, fill.build());
        assertEquals(79, heightmap.getHeight(0, 0));
        assertEquals(79, heightmap.getHeight(15, 15));
    }

    @Test
    void blockEntityStateWithoutDataGetsAnEntry(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var entries = ChunkHack.entries(instance.getChunk(0, 0));

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 60, 1, Block.CHEST.stateId()).build()
        );

        assertEquals(1, result.changed());
        assertTrue(
            entries.containsKey(chunkBlockIndex(1, 60, 1)),
            "a bare chest state still gets its entry"
        );

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertFalse(entries.containsKey(chunkBlockIndex(1, 60, 1)));
        assertEquals(Block.AIR, instance.getBlock(1, 60, 1));
    }

    @Test
    void uniformBlockEntityFillGetsEntriesAndUndoes(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var entries = ChunkHack.entries(instance.getChunk(0, 0));

        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().fillSection(0, 4, 0, Block.CHEST.stateId()).build()
        );

        assertEquals(4096, result.changed());
        assertEquals(4096, entries.size());
        assertEquals(Block.CHEST, instance.getBlock(5, 70, 5));

        ApplyEngine.DEFAULT.apply(instance, result.undo());
        assertTrue(entries.isEmpty(), "undo wipes the fill's entries");
        assertEquals(Block.AIR, instance.getBlock(5, 70, 5));
    }

    /// A chunk with no storage, standing in for a host's outside-the-world chunk.
    private static final class EmptyChunk extends Chunk {
        EmptyChunk(Instance instance, int chunkX, int chunkZ) {
            super(instance, chunkX, chunkZ, false);
        }

        @Override
        public List<Section> getSections() {
            return List.of();
        }

        @Override
        public Section getSection(int section) {
            return new Section();
        }

        @Override
        public Heightmap motionBlockingHeightmap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Heightmap worldSurfaceHeightmap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void loadHeightmapsFromNBT(CompoundBinaryTag heightmaps) {}

        @Override
        public void tick(long time) {}

        @Override
        public SendablePacket getFullDataPacket() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Chunk copy(Instance instance, int chunkX, int chunkZ) {
            return new EmptyChunk(instance, chunkX, chunkZ);
        }

        @Override
        public void reset() {}

        @Override
        public void invalidate() {}

        @Override
        public Block getBlock(int x, int y, int z, Condition condition) {
            return Block.AIR;
        }

        @Override
        public RegistryKey<Biome> getBiome(int x, int y, int z) {
            return Biome.PLAINS;
        }

        @Override
        public void setBiome(int x, int y, int z, RegistryKey<Biome> biome) {}

        @Override
        protected void setBlock(
            int x,
            int y,
            int z,
            Block block,
            BlockHandler.@Nullable Placement placement,
            BlockHandler.@Nullable Destroy destroy
        ) {}
    }
}
