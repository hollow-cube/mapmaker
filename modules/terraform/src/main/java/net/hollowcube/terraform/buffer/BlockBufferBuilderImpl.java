package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.terraform.util.PaletteUtil;
import org.jetbrains.annotations.NotNull;

final class BlockBufferBuilderImpl implements BlockBuffer.Builder {

    private final Long2ObjectMap<Palette> sectionData = new Long2ObjectArrayMap<>();

    @Override
    public void set(int x, int y, int z, int value) {
        long sectionKey = PaletteUtil.packPos(x >> 4, y >> 4, z >> 4);
        Palette section = sectionData.computeIfAbsent(sectionKey, k -> new NaivePalette());
        if (section == null) {
            section = new NaivePalette();
            sectionData.put(sectionKey, section);
        }
        section.set(x & 0xF, y & 0xF, z & 0xF, value);
    }

    @Override
    public @NotNull BlockBuffer build() {
        return new BlockBufferImpl(sectionData);
    }

}
