package net.hollowcube.terraform.buffer;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EditBufferTest {

    // (x, y, z) -> state id. Spans four distinct sections, includes negative coords and air (state 0).
    private static Map<List<Integer>, Integer> sampleEdits() {
        var edits = new HashMap<List<Integer>, Integer>();
        edits.put(List.of(0, 0, 0), 1);
        edits.put(List.of(15, 15, 15), 2); // same section as (0,0,0)
        edits.put(List.of(5, 5, 5), 0); // air, also in section (0,0,0)
        edits.put(List.of(16, 0, 0), 3); // section (1,0,0)
        edits.put(List.of(-1, -1, -1), 4); // section (-1,-1,-1)
        edits.put(List.of(-20, 70, 300), 5); // section (-2,4,18)
        return edits;
    }

    private static EditBuffer build(Map<List<Integer>, Integer> edits) {
        var builder = EditBuffer.builder();
        edits.forEach(
            (pos, state) -> builder.setBlockState(pos.get(0), pos.get(1), pos.get(2), state)
        );
        return builder.build();
    }

    @Test
    void emptyBufferLeavesEverythingUntouched() {
        EditBuffer buffer = EditBuffer.empty();
        assertEquals(0, buffer.sectionCount());
        assertEquals(0, buffer.sizeBytes());
        assertEquals(EditBuffer.NONE, buffer.getBlockState(0, 0, 0));
        assertEquals(EditBuffer.NONE, buffer.getBlockState(-100, 50, 100));
    }

    @Test
    void setThenGet() {
        EditBuffer buffer = build(sampleEdits());
        sampleEdits().forEach(
            (
                pos,
                state
            ) -> assertEquals(
                state.intValue(),
                buffer.getBlockState(pos.get(0), pos.get(1), pos.get(2))
            )
        );
        // Untouched cells in a touched section report NONE.
        assertEquals(EditBuffer.NONE, buffer.getBlockState(1, 1, 1));
        assertEquals(4, buffer.sectionCount());
    }

    @Test
    void forEachPresentVisitsOnlySetCells() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, 1)
            .setBlockState(2, 0, 0, 0) // air, still a set cell
            .build();
        int[] visited = {0};
        buffer.forEachSection((_, _, _, section) -> {
            assertEquals(2, section.count());
            section.forEachBlockState((_, _, _, _) -> visited[0]++);
        });
        assertEquals(2, visited[0]);
    }

    @Test
    void sizeGrowsWithSectionCount() {
        long one = EditBuffer.builder().setBlockState(0, 0, 0, 1).build().sizeBytes();
        long two = EditBuffer.builder()
            .setBlockState(0, 0, 0, 1)
            .setBlockState(100, 0, 0, 1)
            .build()
            .sizeBytes();
        assertTrue(one > 0, "non-empty buffer has positive size");
        assertTrue(two > one, "two sections larger than one");
    }

    // The block-entity size estimate must scale with the NBT payload, not be a flat per-BE constant: a
    // chest of items / long command / written book holds kilobytes the streaming-undo and memory-budget
    // bounds would otherwise be blind to (the old model counted a flat 64 B, "NBT not measured").
    @Test
    void blockEntitySizeScalesWithNbtPayload() {
        Block small = Block.OAK_SIGN.withNbt(
            CompoundBinaryTag.builder().putString("Text", "hi").build()
        );
        Block large = Block.OAK_SIGN.withNbt(
            CompoundBinaryTag.builder().putString("Text", "x".repeat(5000)).build()
        );

        long smallBytes = EditBuffer.builder().setBlockState(0, 0, 0, small).build().sizeBytes();
        long largeBytes = EditBuffer.builder().setBlockState(0, 0, 0, large).build().sizeBytes();
        assertTrue(
            largeBytes >= smallBytes + 9000,
            "a 5000-char NBT string must dominate the size estimate, not flatten to a constant: "
                + smallBytes
                + " vs "
                + largeBytes
        );
    }

    @Test
    void fillSectionIsUniformFullCover() {
        EditBuffer buffer = EditBuffer.builder()
            .fillSection(0, 0, 0, Block.STONE.stateId())
            .build();

        assertEquals(1, buffer.sectionCount());
        assertEquals(Block.STONE.stateId(), buffer.getBlockState(0, 0, 0));
        assertEquals(Block.STONE.stateId(), buffer.getBlockState(15, 15, 15));
        assertEquals(EditBuffer.NONE, buffer.getBlockState(16, 0, 0)); // next section untouched
        buffer.forEachSection((_, _, _, section) -> {
            assertEquals(4096, section.count()); // full cover
            assertEquals(
                Block.STONE.stateId() + 1,
                section.uniformBlockValue()
            ); // offset-encoded uniform
        });
    }

    @Test
    void blockEntityChannelKeepsNbt() {
        CompoundBinaryTag nbt = CompoundBinaryTag.builder()
            .putString("Text", "hello")
            .putInt("n", 7)
            .build();
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(4, 5, 6, Block.OAK_SIGN.withNbt(nbt))
            .setBlockState(0, 0, 0, Block.STONE)
            .build();

        assertEquals(Block.OAK_SIGN.stateId(), buffer.getBlockState(4, 5, 6));
        Block blockEntity = buffer.getBlock(4, 5, 6);
        assertNotNull(blockEntity);
        assertEquals(nbt, blockEntity.nbt());
    }

    @Test
    void overwritingABlockEntityCellWithAPlainBlockDropsItsData() {
        Block sign = Block.OAK_SIGN.withNbt(
            CompoundBinaryTag.builder().putString("Text", "x").build()
        );
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(1, 1, 1, sign)
            .setBlockState(1, 1, 1, Block.STONE)
            .build();
        assertEquals(Block.STONE, buffer.getBlock(1, 1, 1));
    }

    @Test
    void overwritingABlockEntityCellWithAStateIdDropsItsData() {
        Block sign = Block.OAK_SIGN.withNbt(
            CompoundBinaryTag.builder().putString("Text", "x").build()
        );
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(1, 1, 1, sign)
            .setBlockState(1, 1, 1, Block.STONE.stateId())
            .build();

        assertEquals(Block.STONE, buffer.getBlock(1, 1, 1));
        EditBuffer viaBlock = EditBuffer.builder()
            .setBlockState(1, 1, 1, sign)
            .setBlockState(1, 1, 1, Block.STONE)
            .build();
        assertEquals(
            viaBlock.sizeBytes(),
            buffer.sizeBytes(),
            "NBT bytes released like the Block overload"
        );
        buffer.forEachSection((_, _, _, section) -> assertFalse(section.hasBlockEntities()));
    }

    @Test
    void stateIdsAboveShortRangeSurviveSparseAndDense() {
        int highState = highestStateId();
        assertTrue(highState > Short.MAX_VALUE, "precondition: registry has state ids past 32767");

        EditBuffer sparse = EditBuffer.builder().setBlockState(0, 0, 0, highState).build();
        assertEquals(highState, sparse.getBlockState(0, 0, 0));

        var builder = EditBuffer.builder();
        for (int i = 0; i <= 256; i++)
            builder.setBlockState(i & 15, (i >> 4) & 15, i >> 8, i == 0 ? highState : 1);
        EditBuffer dense = builder.build();
        assertEquals(highState, dense.getBlockState(0, 0, 0));
        assertEquals(1, dense.getBlockState(1, 0, 0));
    }

    private static int highestStateId() {
        int max = 0;
        for (Block block : Block.values())
            for (Block state : block.possibleStates()) max = Math.max(max, state.stateId());
        return max;
    }

    @Test
    void biomesAreIndependentOfBlocks() {
        EditBuffer buffer = EditBuffer.builder()
            .setBiome(0, 0, 0, 5)
            .setBiome(40, 8, 40, 9) // a different section, biome-only (no blocks set there)
            .build();

        assertEquals(5, buffer.getBiome(0, 0, 0));
        assertEquals(9, buffer.getBiome(40, 8, 40));
        assertEquals(EditBuffer.NONE, buffer.getBiome(8, 0, 0)); // untouched cell
        assertEquals(
            EditBuffer.NONE,
            buffer.getBlockState(40, 8, 40)
        ); // biome set, block untouched
    }

    @Test
    void blockOverloadStoresStateId() {
        EditBuffer buffer = EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build();
        assertEquals(Block.STONE.stateId(), buffer.getBlockState(0, 0, 0));
    }

    @Test
    void plainBlockHasNoBlockEntity() {
        EditBuffer buffer = EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build();
        // A plain set cell returns the plain block itself (no NBT/handler), not null.
        assertEquals(Block.STONE, buffer.getBlock(0, 0, 0));
    }

    @Test
    void biomeIsCellGranular() {
        EditBuffer buffer = EditBuffer.builder().setBiome(0, 0, 0, 5).build();
        assertEquals(5, buffer.getBiome(3, 3, 3)); // same 4³ cell
        assertEquals(EditBuffer.NONE, buffer.getBiome(4, 0, 0)); // next cell
    }

    @Test
    void builderIsSingleUse() {
        var builder = EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE);
        EditBuffer buffer = builder.build();

        assertThrows(IllegalStateException.class, () -> builder.setBlockState(1, 0, 0, Block.DIRT));
        assertThrows(IllegalStateException.class, () -> builder.setBiome(0, 0, 0, 1));
        assertThrows(IllegalStateException.class, () -> builder.fillSection(1, 0, 0, 1));
        assertThrows(IllegalStateException.class, builder::build);
        assertEquals(EditBuffer.NONE, buffer.getBlockState(1, 0, 0));
    }
}
