package net.hollowcube.terraform.legacy.buffer;

import net.hollowcube.terraform.buffer.BufferSection;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.legacy.buffer.palette.FixedPalette;
import net.hollowcube.terraform.legacy.buffer.palette.Palette;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A legacy {@link BlockBuffer} backed by a new-engine {@link EditBuffer}, so history and results hold the
 * new representation and re-applying one skips conversion.
 */
public record EditBlockBuffer(@NotNull EditBuffer edit) implements BlockBuffer {

    /**
     * The buffer as an {@link EditBuffer}: unwrapped when it already is one, else converted cell by cell.
     */
    public static @NotNull EditBuffer toEditBuffer(@NotNull BlockBuffer buffer) {
        if (buffer instanceof EditBlockBuffer(EditBuffer edit)) return edit;

        var builder = EditBuffer.builder();
        buffer.forEachSection((sectionX, sectionY, sectionZ, palette) -> {
            if (palette instanceof FixedPalette(int value)) {
                if (value != Palette.UNSET) builder.fillSection(sectionX, sectionY, sectionZ, value);
                return;
            }

            int baseX = sectionX << 4, baseY = sectionY << 4, baseZ = sectionZ << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        var block = palette.get(x, y, z);
                        if (block != null) builder.setBlockState(baseX + x, baseY + y, baseZ + z, block);
                    }
                }
            }
        });
        return builder.build();
    }

    @Override
    public void forEachSection(@NotNull SectionConsumer consumer) {
        edit.forEachSection((sectionX, sectionY, sectionZ, section) ->
                consumer.accept(sectionX, sectionY, sectionZ, new SectionPalette(section)));
    }

    @Override
    public long sizeBytes() {
        return edit.sizeBytes();
    }

    private record SectionPalette(@NotNull BufferSection section) implements Palette {
        @Override
        public @Nullable Block get(int x, int y, int z) {
            var blockEntity = section.getBlockEntity(x, y, z);
            if (blockEntity != null) return blockEntity;
            int stateId = section.getBlockState(x, y, z);
            return stateId == BufferSection.NONE ? null : Block.fromStateId(stateId);
        }

        @Override
        public long sizeBytes() {
            return 0; // Counted once for the whole buffer by EditBlockBuffer#sizeBytes
        }
    }
}
