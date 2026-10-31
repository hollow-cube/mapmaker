package net.hollowcube.terraform.legacy.buffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.legacy.buffer.palette.FixedPalette;
import net.hollowcube.terraform.legacy.buffer.palette.Palette;
import net.hollowcube.terraform.legacy.util.PaletteUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditBlockBufferTest {

    @Test
    void convertsCellsAndBlockEntities() throws InterruptedException {
        var chest = Block.CHEST.withNbt(CompoundBinaryTag.builder().putString("CustomName", "loot").build());
        var builder = BlockBuffer.builder(null);
        builder.set(-1, 5, 17, Block.STONE);
        builder.set(3, -20, 4, chest);
        var edit = EditBlockBuffer.toEditBuffer(builder.build());

        assertEquals(Block.STONE, edit.getBlock(-1, 5, 17));
        assertEquals(chest, edit.getBlock(3, -20, 4));
        assertNull(edit.getBlock(0, 5, 17));
    }

    @Test
    void convertsFixedPalettesToFilledSections() {
        var sections = new Long2ObjectArrayMap<Palette>();
        sections.put(PaletteUtil.packPos(1, 2, 3), new FixedPalette(Block.DIRT.stateId()));
        sections.put(PaletteUtil.packPos(4, 5, 6), new FixedPalette(Palette.UNSET));
        var edit = EditBlockBuffer.toEditBuffer(new BlockBufferImpl(sections));

        assertEquals(1, edit.sectionCount());
        assertEquals(Block.DIRT.stateId(), edit.getBlockState(16, 32, 48));
        assertEquals(Block.DIRT.stateId(), edit.getBlockState(31, 47, 63));
    }

    @Test
    void wrappedBufferUnwrapsAndReadsBack() {
        var chest = Block.CHEST.withNbt(CompoundBinaryTag.builder().putInt("x", 1).build());
        var edit = EditBuffer.builder()
                .setBlockState(-1, 0, 0, Block.STONE.stateId())
                .setBlockState(2, 3, 4, chest)
                .build();
        var wrapped = new EditBlockBuffer(edit);

        assertSame(edit, EditBlockBuffer.toEditBuffer(wrapped));
        int[] cells = {0};
        wrapped.forEachSection((sectionX, sectionY, sectionZ, palette) -> {
            if (sectionX == -1) {
                assertEquals(Block.STONE, palette.get(15, 0, 0));
                assertNull(palette.get(14, 0, 0));
                cells[0]++;
            } else {
                assertEquals(chest, palette.get(2, 3, 4));
                cells[0]++;
            }
        });
        assertEquals(2, cells[0]);
    }
}
