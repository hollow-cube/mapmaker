package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.terraform.util.PaletteUtil;
import org.jetbrains.annotations.NotNull;

final class BlockBufferImpl implements BlockBuffer {
    private final Long2ObjectMap<Palette> sectionData;

    BlockBufferImpl(@NotNull Long2ObjectMap<Palette> sectionData) {
        this.sectionData = sectionData;
    }

    @Override
    public void forEachSection(@NotNull SectionConsumer consumer) {
        sectionData.forEach((packedPos, palette) -> {
            int x = PaletteUtil.unpackX(packedPos);
            int y = PaletteUtil.unpackY(packedPos);
            int z = PaletteUtil.unpackZ(packedPos);
            consumer.accept(x, y, z, palette);
        });
    }

    @Override
    public long sizeBytes() {
        long total = 0;
        for (var palette : sectionData.values())
            total += palette.sizeBytes();
        return (int) total;
    }
}
