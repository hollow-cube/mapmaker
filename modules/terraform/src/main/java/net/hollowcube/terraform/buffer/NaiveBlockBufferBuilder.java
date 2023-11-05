package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.terraform.task.Task;
import net.hollowcube.terraform.task.edit.WorldView;
import net.hollowcube.terraform.util.PaletteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class NaiveBlockBufferBuilder implements BlockBuffer.Builder {

    private final Long2ObjectMap<Palette> sectionData = new Long2ObjectArrayMap<>();
    private final @Nullable WorldView world;

    private boolean hasBorderTaint = false;

    public NaiveBlockBufferBuilder(@Nullable WorldView world) {
        this.world = world;
    }

    @Override
    public void set(int x, int y, int z, int value) {
        // Ensure the position is within the world border
        if (world != null && !world.contains(x, y, z)) {
            if (!hasBorderTaint) {
                hasBorderTaint = true;
                world.task().addAttribute(Task.ATT_BORDER_TAINT);
            }
            return;
        }

        // Set the block
        long sectionKey = PaletteUtil.packPos(x >> 4, y >> 4, z >> 4);
        //todo how can this computeIfAbsent call be sped up? It is almost all of the time when building a big buffer
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
