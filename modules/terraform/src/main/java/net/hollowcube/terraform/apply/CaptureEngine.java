package net.hollowcube.terraform.apply;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palette;

import java.util.List;

import static net.minestom.server.coordinate.CoordConversion.*;

/// Reads the world into an [EditBuffer]
@SuppressWarnings("UnstableApiUsage")
public final class CaptureEngine {

    public static EditBuffer capture(Instance instance, Area.Cuboid box) {
        BlockVec min = box.min(), max = box.max();
        var out = EditBuffer.builder();
        for (int cz = min.blockZ() >> 4; cz <= max.blockZ() >> 4; cz++) {
            for (int cx = min.blockX() >> 4; cx <= max.blockX() >> 4; cx++) {
                Chunk chunk = instance.getChunk(cx, cz);
                if (chunk == null) chunk = instance.loadChunk(cx, cz).join();
                // Chunks without block storage (outside the world border) have nothing to capture.
                if (!(chunk instanceof DynamicChunk)) continue;

                chunk.lockReadLock();
                try {
                    captureChunk(chunk, min, max, out);
                } finally {
                    chunk.unlockReadLock();
                }
            }
        }
        return out.build();
    }

    private static void captureChunk(
        Chunk chunk,
        BlockVec min,
        BlockVec max,
        EditBuffer.Builder out
    ) {
        int cx = chunk.getChunkX(), cz = chunk.getChunkZ();
        int minSection = Math.max(chunk.getMinSection(), min.blockY() >> 4);
        int maxSection = Math.min(chunk.getMaxSection() - 1, max.blockY() >> 4);
        if (minSection > maxSection) return;

        int x0 = Math.max(min.blockX(), cx << 4), x1 = Math.min(max.blockX(), (cx << 4) + 15);
        int z0 = Math.max(min.blockZ(), cz << 4), z1 = Math.min(max.blockZ(), (cz << 4) + 15);
        boolean columnCovered = x0 == cx << 4
            && x1 == (cx << 4) + 15
            && z0 == cz << 4
            && z1 == (cz << 4) + 15;
        for (int sy = minSection; sy <= maxSection; sy++) {
            Palette blocks = chunk.getSection(sy).blockPalette();
            int y0 = Math.max(min.blockY(), sy << 4), y1 = Math.min(max.blockY(), (sy << 4) + 15);
            if (columnCovered && y0 == sy << 4 && y1 == (sy << 4) + 15) {
                out.captureSection(cx, sy, cz, blocks);
                continue;
            }
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        out.setBlockState(x, y, z, blocks.get(x & 15, y & 15, z & 15));
                    }
                }
            }
        }

        // Every captured cell's section is in the buffer now, so the entries only add their data.
        int minY = Math.max(min.blockY(), minSection << 4);
        int maxY = Math.min(max.blockY(), (maxSection << 4) + 15);
        Int2ObjectMap<Block> entries = ChunkHack.entries(chunk);
        for (Int2ObjectMap.Entry<Block> entry : Int2ObjectMaps.fastIterable(entries)) {
            int index = entry.getIntKey();
            int x = (cx << 4) + chunkBlockIndexGetX(index);
            int y = chunkBlockIndexGetY(index);
            int z = (cz << 4) + chunkBlockIndexGetZ(index);
            if (x < x0 || x > x1 || y < minY || y > maxY || z < z0 || z > z1) continue;
            out.captureBlockEntity(x, y, z, entry.getValue());
        }
    }

    /// Captures the entirety of each `sectionY`s into `out`
    static void captureSections(Chunk chunk, int[] sectionYs, EditBuffer.Builder out) {
        if (!(chunk instanceof DynamicChunk)) return;

        int cx = chunk.getChunkX(), cz = chunk.getChunkZ();
        chunk.lockReadLock();
        try {
            IntSet captured = new IntOpenHashSet(sectionYs.length);
            for (int sy : sectionYs) {
                if (sy < chunk.getMinSection() || sy >= chunk.getMaxSection()) continue;

                Section section = chunk.getSection(sy);
                out.captureSection(cx, sy, cz, section.blockPalette());
                out.captureBiomes(cx, sy, cz, section.biomePalette());
                captured.add(sy);
            }

            Int2ObjectMap<Block> entries = ChunkHack.entries(chunk);
            for (Int2ObjectMap.Entry<Block> entry : Int2ObjectMaps.fastIterable(entries)) {
                int index = entry.getIntKey();
                int y = chunkBlockIndexGetY(index);
                if (!captured.contains(y >> 4)) continue;
                int x = (cx << 4) + chunkBlockIndexGetX(index);
                int z = (cz << 4) + chunkBlockIndexGetZ(index);
                out.captureBlockEntity(x, y, z, entry.getValue());
            }
        } finally {
            chunk.unlockReadLock();
        }
    }

    /// Captures the marked cells of each section: block states with their block entities, and biomes.
    static void captureCells(Chunk chunk, List<SectionChange> sections, EditBuffer.Builder out) {
        if (!(chunk instanceof DynamicChunk)) return;

        int baseX = chunk.getChunkX() << 4, baseZ = chunk.getChunkZ() << 4;
        chunk.lockReadLock();
        try {
            Int2ObjectMap<Block> entries = ChunkHack.entries(chunk);
            for (SectionChange change : sections) {
                int sectionY = change.sectionY();
                if (sectionY < chunk.getMinSection() || sectionY >= chunk.getMaxSection()) continue;

                int baseY = sectionY << 4;
                Section section = chunk.getSection(sectionY);
                Palette blockPalette = section.blockPalette(),
                    biomePalette = section.biomePalette();
                change.blocks()
                    .stream()
                    .forEach(index -> {
                        int x = baseX + (index & 15),
                            y = baseY + (index >> 8),
                            z = baseZ + ((index >> 4) & 15);
                        Block entry = entries.get(chunkBlockIndex(x, y, z));
                        if (entry != null) out.setBlockState(x, y, z, entry);
                        else out.setBlockState(x, y, z, blockPalette.get(x & 15, y & 15, z & 15));
                    });
                change.biomes()
                    .stream()
                    .forEach(index -> {
                        int bx = index & 3, by = index >> 4, bz = (index >> 2) & 3;
                        out.setBiome(
                            baseX + (bx << 2),
                            baseY + (by << 2),
                            baseZ + (bz << 2),
                            biomePalette.get(bx, by, bz)
                        );
                    });
            }
        } finally {
            chunk.unlockReadLock();
        }
    }

    private CaptureEngine() {}
}
