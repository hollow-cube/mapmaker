package net.hollowcube.terraform.legacy.buffer;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditBlockBufferTest {

    @Test
    void builderBuildsAnEditBufferDirectly() throws InterruptedException {
        var chest = Block.CHEST.withNbt(CompoundBinaryTag.builder().putString("CustomName", "loot").build());
        var builder = BlockBuffer.builder(null);
        builder.set(-1, 5, 17, Block.STONE);
        builder.set(3, -20, 4, chest);
        builder.set(40, 0, 0, Block.DIRT.stateId());
        var built = builder.build();

        var edit = assertInstanceOf(EditBlockBuffer.class, built).edit();
        assertSame(edit, EditBlockBuffer.toEditBuffer(built));
        assertEquals(Block.STONE, edit.getBlock(-1, 5, 17));
        assertEquals(chest, edit.getBlock(3, -20, 4));
        assertEquals(Block.DIRT, edit.getBlock(40, 0, 0));
        assertNull(edit.getBlock(0, 5, 17));
    }

    @Test
    void boundedBuilderRejectsCellsOutsideItsBounds() throws InterruptedException {
        var builder = BlockBuffer.builder(null, new Vec(2, 2, 2), new Vec(-2, -2, -2));
        builder.set(-2, 2, 0, 1);
        builder.set(2, -2, 2, Block.STONE);

        assertThrows(IllegalArgumentException.class, () -> builder.set(3, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> builder.set(0, -3, 0, Block.STONE));
        var edit = EditBlockBuffer.toEditBuffer(builder.build());
        assertEquals(1, edit.getBlockState(-2, 2, 0));
        assertEquals(Block.STONE, edit.getBlock(2, -2, 2));
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
