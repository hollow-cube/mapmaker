package net.hollowcube.terraform.apply;

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.palette.Palette;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

/// Updates a chunk's [HeightmapView]s once an apply has written it, under its write lock.
///
/// Writes only record the span of y written in each column. Afterwards each written column is scanned
/// inward from whichever is further out, its old surface or its outermost written cell. Everything beyond
/// that point was neither written nor inside the old surface, so the first matching block found is the new
/// surface.
final class HeightmapChanges {
    private final Chunk chunk;
    private final Function<Chunk, Collection<HeightmapView>> provider;
    private final int minY;
    private final int maxY;
    private final int none;
    private final int[] lowest = new int[256];
    private final int[] highest = new int[256];
    private boolean anyWritten;

    HeightmapChanges(Chunk chunk, Function<Chunk, Collection<HeightmapView>> provider) {
        this.chunk = chunk;
        this.provider = provider;
        this.minY = chunk.getMinSection() << 4;
        this.maxY = chunk.getMaxSection() << 4;
        this.none = minY - 1;
        Arrays.fill(lowest, Integer.MAX_VALUE);
        Arrays.fill(highest, Integer.MIN_VALUE);
    }

    /// A cell's block state changed.
    void blockChanged(int x, int y, int z) {
        written(x | (z << 4), y, y);
    }

    /// Every cell of the section was rewritten.
    void sectionChanged(int sectionY) {
        int baseY = sectionY << 4;
        for (int column = 0; column < 256; column++) written(column, baseY, baseY + 15);
    }

    /// Rescans the written columns of every heightmap. Call once the whole chunk is written.
    void refresh() {
        if (!anyWritten) return;
        for (HeightmapView view : provider.apply(chunk)) {
            Boolean[] sections = classify(view);
            for (int column = 0; column < 256; column++) {
                if (highest[column] < lowest[column]) continue;
                int x = column & 15, z = column >> 4;
                int height = view.height(x, z);
                int surface = scan(view, x, z, startY(view, column, height), sections);
                if (surface != height) view.setHeight(x, z, surface);
            }
        }
    }

    private void written(int column, int low, int high) {
        anyWritten = true;
        lowest[column] = Math.min(lowest[column], low);
        highest[column] = Math.max(highest[column], high);
    }

    /// Per section, whether every cell matches the view, none does, or null when it varies. Tested once per
    /// section rather than once per column scanned through it.
    private Boolean[] classify(HeightmapView view) {
        int minSection = chunk.getMinSection();
        var sections = new Boolean[chunk.getMaxSection() - minSection];
        for (int i = 0; i < sections.length; i++) {
            Palette palette = chunk.getSection(minSection + i).blockPalette();
            if (palette.bitsPerEntry() == 0) sections[i] = view.matches(palette.singleValue());
            // A zero count is all air even if per-cell removals left the palette packed.
            else if (palette.count() == 0) sections[i] = view.matches(0);
        }
        return sections;
    }

    /// Where a column's scan starts: its old surface or its outermost written cell, whichever is further out.
    private int startY(HeightmapView view, int column, int height) {
        if (!view.bottomUp()) return Math.max(height, highest[column]);
        // An empty bottom-up column also reads `none`, which lies below every cell rather than above.
        return height == none ? lowest[column] : Math.min(height, lowest[column]);
    }

    /// Walks inward from `startY` to the first matching block, or returns `none`.
    private int scan(HeightmapView view, int x, int z, int startY, Boolean[] sections) {
        int step = view.bottomUp() ? 1 : -1;
        int y = startY;
        while (y >= minY && y < maxY) {
            int sectionY = y >> 4;
            Boolean matches = sections[sectionY - (minY >> 4)];
            if (matches == null) {
                Palette palette = chunk.getSection(sectionY).blockPalette();
                for (; y >> 4 == sectionY; y += step) {
                    if (view.matches(palette.get(x, y & 15, z))) return y;
                }
            } else if (matches) {
                return y;
            } else {
                y = step > 0 ? (sectionY + 1) << 4 : (sectionY << 4) - 1;
            }
        }
        return none;
    }
}
