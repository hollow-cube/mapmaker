package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import net.hollowcube.terraform.buffer.palette.Palette;
import net.hollowcube.terraform.task.Task;
import net.hollowcube.terraform.task.edit.WorldView;
import net.hollowcube.terraform.util.PaletteUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.hollowcube.terraform.util.math.CoordinateUtil.*;

final class BoundedBlockBufferBuilder implements BlockBuffer.Builder {
    private @Nullable WorldView world;
    private final Point min, max; // Absolute min/max
    private final Point smin, smax; // Section min/max
    private final Palette.Mutable[] sectionData;

    private boolean hasBorderTaint = false;


    public BoundedBlockBufferBuilder(@Nullable WorldView world, @NotNull Point pos1, @NotNull Point pos2) {
        this.world = world;
        this.min = floor(min(pos1, pos2));
        this.max = floor(max(pos1, pos2));
        this.smin = toRelative(min);
        this.smax = toRelative(max);

        var sectionCount = (smax.blockX() - smin.blockX() + 1)
                * (smax.blockY() - smin.blockY() + 1)
                * (smax.blockZ() - smin.blockZ() + 1);
        this.sectionData = new Palette.Mutable[sectionCount];
    }

    @Override
    public void set(int x, int y, int z, int value) {
        set(new Vec(x, y, z), value);
    }

    @Override
    public void set(@NotNull Point point, int value) {
        Check.argCondition(!contains(point), "Point {0} is not within the bounds of this builder ({1} to {2})", point, min, max);

        // Ensure the position is within the world border
        if (world != null && !world.contains(point.blockX(), point.blockY(), point.blockZ())) {
            if (!hasBorderTaint) {
                hasBorderTaint = true;
                world.task().addAttribute(Task.ATT_BORDER_TAINT);
            }
            return;
        }

        var section = toRelative(point);
        var sectionIndex = (section.blockX() - smin.blockX())
                + (section.blockY() - smin.blockY()) * (smax.blockX() - smin.blockX() + 1)
                + (section.blockZ() - smin.blockZ()) * (smax.blockX() - smin.blockX() + 1) * (smax.blockY() - smin.blockY() + 1);

        var palette = sectionData[sectionIndex];
        if (palette == null) {
            palette = Palette.blocks();
            sectionData[sectionIndex] = palette;
        }

        palette.set(point.blockX() & 0xF, point.blockY() & 0xF, point.blockZ() & 0xF, value);
    }

    @Override
    public void set(int x, int y, int z, @Nullable Block value) {
        set(new Vec(x, y, z), value);
    }

    @Override
    public void set(@NotNull Point point, @Nullable Block value) {
        Check.argCondition(!contains(point), "Point {0} is not within the bounds of this builder ({1} to {2})", point, min, max);

        // Ensure the position is within the world border
        if (world != null && !world.contains(point.blockX(), point.blockY(), point.blockZ())) {
            if (!hasBorderTaint) {
                hasBorderTaint = true;
                world.task().addAttribute(Task.ATT_BORDER_TAINT);
            }
            return;
        }

        var section = toRelative(point);
        var sectionIndex = (section.blockX() - smin.blockX())
                + (section.blockY() - smin.blockY()) * (smax.blockX() - smin.blockX() + 1)
                + (section.blockZ() - smin.blockZ()) * (smax.blockX() - smin.blockX() + 1) * (smax.blockY() - smin.blockY() + 1);

        var palette = sectionData[sectionIndex];
        if (palette == null) {
            palette = Palette.blocks();
            sectionData[sectionIndex] = palette;
        }

        palette.set(point.blockX() & 0xF, point.blockY() & 0xF, point.blockZ() & 0xF, value);
    }

    @Override
    public @NotNull BlockBuffer build() {
        var sectionMap = new Long2ObjectArrayMap<Palette>();
        for (int i = 0; i < sectionData.length; i++) {
            var palette = sectionData[i];
            if (palette != null) {
                var section = smin.add(i % (smax.blockX() - smin.blockX() + 1),
                        (i / (smax.blockX() - smin.blockX() + 1)) % (smax.blockY() - smin.blockY() + 1),
                        i / ((smax.blockX() - smin.blockX() + 1) * (smax.blockY() - smin.blockY() + 1)));
                var sectionKey = PaletteUtil.packPos(section.blockX(), section.blockY(), section.blockZ());
                sectionMap.put(sectionKey, palette);
            }
        }
        return new BlockBufferImpl(sectionMap);
    }

    private boolean contains(@NotNull Point point) {
        return point.blockX() >= min.blockX() && point.blockX() <= max.blockX()
                && point.blockY() >= min.blockY() && point.blockY() <= max.blockY()
                && point.blockZ() >= min.blockZ() && point.blockZ() <= max.blockZ();
    }

    private @NotNull Point toRelative(@NotNull Point point) {
        return new Vec(point.blockX() >> 4, point.blockY() >> 4, point.blockZ() >> 4);
    }
}
