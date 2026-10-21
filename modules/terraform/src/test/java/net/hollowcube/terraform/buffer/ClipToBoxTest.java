package net.hollowcube.terraform.buffer;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The world-border clip primitive ([EditBuffer#clipToBox]): positional, blocks and biomes, and
/// copy-on-write (the source buffer may be shared with history and must never be mutated).
class ClipToBoxTest {

    @Test
    void keepsInsideDropsOutsideWithInclusiveEdges() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .setBlockState(5, 5, 5, Block.DIRT)
            .setBlockState(10, 10, 10, Block.GRANITE) // exactly the inclusive max: kept
            .setBlockState(11, 5, 5, Block.DIORITE) // one past: dropped
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 10, 10, 10);

        assertEquals(Block.STONE.stateId(), clipped.getBlockState(0, 0, 0));
        assertEquals(Block.DIRT.stateId(), clipped.getBlockState(5, 5, 5));
        assertEquals(Block.GRANITE.stateId(), clipped.getBlockState(10, 10, 10));
        assertEquals(EditBuffer.NONE, clipped.getBlockState(11, 5, 5));
    }

    @Test
    void returnsSameBufferWhenNothingNeedsClipping() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(1, 1, 1, Block.STONE)
            .setBlockState(14, 14, 14, Block.DIRT)
            .build();

        // The box covers the whole occupied section exactly (inclusive [0,15]).
        assertSame(buffer, buffer.clipToBox(0, 0, 0, 15, 15, 15));
        // And a much larger box, trivially.
        assertSame(buffer, buffer.clipToBox(-64, -64, -64, 63, 63, 63));
    }

    @Test
    void emptyBufferReturnsItself() {
        EditBuffer empty = EditBuffer.empty();
        assertSame(empty, empty.clipToBox(0, 0, 0, 1, 1, 1));
    }

    @Test
    void denseBoundarySectionWithBlockEntitiesIsCopiedNotMutated() {
        Block chest = Block.CHEST.withNbt(
            CompoundBinaryTag.builder().putString("Lock", "k").build()
        );
        var builder = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 2; y++)
                    builder.setBlockState(x, y, z, Block.STONE); // 512 cells: dense
        builder.setBlockState(12, 0, 0, chest);
        EditBuffer buffer = builder.build();
        long sizeBefore = buffer.sizeBytes();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 7, 15, 15);

        assertEquals(EditBuffer.NONE, clipped.getBlockState(12, 0, 0));
        assertNull(clipped.getBlock(12, 0, 0));
        assertEquals(Block.STONE.stateId(), clipped.getBlockState(7, 1, 15));

        assertEquals(
            Block.CHEST.stateId(),
            buffer.getBlockState(12, 0, 0),
            "source dense palette untouched"
        );
        assertNotNull(buffer.getBlock(12, 0, 0), "source block entity untouched");
        assertEquals(Block.STONE.stateId(), buffer.getBlockState(15, 1, 15));
        assertEquals(sizeBefore, buffer.sizeBytes());
    }

    @Test
    void sourceBufferIsNeverMutated() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(2, 2, 2, Block.STONE)
            .setBlockState(12, 2, 2, Block.DIRT)
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 7, 7, 7);

        assertNotSame(buffer, clipped);
        assertEquals(
            EditBuffer.NONE,
            clipped.getBlockState(12, 2, 2),
            "clipped copy drops the cell"
        );
        assertEquals(
            Block.DIRT.stateId(),
            buffer.getBlockState(12, 2, 2),
            "source keeps the out-of-box cell (copy-on-write)"
        );
        assertEquals(Block.STONE.stateId(), buffer.getBlockState(2, 2, 2));
    }

    @Test
    void whollyOutsideSectionIsDroppedWholesale() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(1, 1, 1, Block.STONE) // section (0,0,0)
            .setBlockState(40, 1, 1, Block.DIRT) // section (2,0,0), wholly outside
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 15, 15, 15);

        assertEquals(1, clipped.sectionCount());
        assertEquals(Block.STONE.stateId(), clipped.getBlockState(1, 1, 1));
        assertEquals(EditBuffer.NONE, clipped.getBlockState(40, 1, 1));
    }

    @Test
    void boundarySectionEmptiedByTheClipIsRemoved() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(1, 1, 1, Block.STONE) // section (0,0,0), inside
            .setBlockState(
                17,
                1,
                1,
                Block.DIRT
            ) // section (1,0,0): straddled by the box, cell outside
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 16, 15, 15);

        assertEquals(1, clipped.sectionCount(), "emptied boundary section removed");
        assertEquals(EditBuffer.NONE, clipped.getBlockState(17, 1, 1));
    }

    @Test
    void negativeCoordinateBoxClipsCorrectly() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(-5, -5, -5, Block.STONE)
            .setBlockState(-20, -5, -5, Block.DIRT)
            .setBlockState(3, 60, 3, Block.GRANITE)
            .build();

        EditBuffer clipped = buffer.clipToBox(-10, -64, -10, 9, 100, 9);

        assertEquals(Block.STONE.stateId(), clipped.getBlockState(-5, -5, -5));
        assertEquals(EditBuffer.NONE, clipped.getBlockState(-20, -5, -5));
        assertEquals(Block.GRANITE.stateId(), clipped.getBlockState(3, 60, 3));
    }

    @Test
    void blockEntityRidesWithItsCell() {
        Block chest = Block.CHEST.withNbt(
            CompoundBinaryTag.builder().putString("Lock", "k").build()
        );
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(2, 2, 2, chest)
            .setBlockState(12, 2, 2, chest)
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 7, 7, 7);

        Block kept = clipped.getBlock(2, 2, 2);
        assertNotNull(kept);
        assertTrue(kept.hasNbt(), "in-box block entity keeps its NBT");
        assertNull(clipped.getBlock(12, 2, 2), "out-of-box block entity dropped with its cell");
        assertNotNull(buffer.getBlock(12, 2, 2), "source block entity untouched");
    }

    @Test
    void biomeCellsClipByFootprintIntersection() {
        EditBuffer buffer = EditBuffer.builder()
            .setBiome(0, 0, 0, 7) // footprint 0..3: intersects the box
            .setBiome(4, 0, 0, 7) // footprint 4..7: intersects (box maxX 5)
            .setBiome(8, 0, 0, 7) // footprint 8..11: outside
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 5, 15, 15);

        assertEquals(7, clipped.getBiome(0, 0, 0));
        assertEquals(7, clipped.getBiome(4, 0, 0), "straddling biome cell kept (4³ granularity)");
        assertEquals(EditBuffer.NONE, clipped.getBiome(8, 0, 0));
        assertEquals(7, buffer.getBiome(8, 0, 0), "source biome untouched");
    }

    @Test
    void biomeOnlySectionSurvivesWhenItsBiomesDo() {
        EditBuffer buffer = EditBuffer.builder()
            .setBiome(2, 2, 2, 3)
            .setBlockState(30, 2, 2, Block.STONE) // separate, outside section
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 15, 15, 15);

        assertEquals(1, clipped.sectionCount());
        assertEquals(3, clipped.getBiome(2, 2, 2));
    }

    @Test
    void whollyOutsideSectionDropsItsBiomesToo() {
        EditBuffer buffer = EditBuffer.builder()
            .setBiome(40, 2, 2, 3) // section (2,0,0)
            .build();

        EditBuffer clipped = buffer.clipToBox(0, 0, 0, 15, 15, 15);

        assertEquals(0, clipped.sectionCount());
        assertEquals(EditBuffer.NONE, clipped.getBiome(40, 2, 2));
    }
}
