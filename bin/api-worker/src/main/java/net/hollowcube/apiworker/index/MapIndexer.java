package net.hollowcube.apiworker.index;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.hollowcube.polar.PolarSection;

import java.util.Arrays;
import java.util.List;

/// Computes a map's [MapFeatures] from its world bytes.
public final class MapIndexer {
    /// The field set and how it is computed. Bumped whenever a row written by the old code would
    /// differ from one written by the new, so a backfill can find the rows behind.
    public static final int FEATURE_VERSION = 1;

    private static final int CELL_SIZE = 8;
    private static final int CELLS_PER_SECTION = 8; // 2x2x2 cells per 16^3 section

    /// Trimmed from each end of the extent to account for stray blocks and decoration.
    private static final double EXTENT_TRIM_FACTOR = 0.01;

    /// Runs the class initialisation now — Minestom's registries, the datafixer model, the action
    /// codecs — so a build missing something they need fails at boot rather than on the first
    /// row, where an `Error` would escape the worker's reporting.
    public static void init() {
        PolarHelper.CODER.getClass();
    }

    public static MapFeatures index(byte[] worldData) {
        var world = PolarHelper.read(worldData);

        var cells = new LongOpenHashSet();
        var cellX = new IntArrayList();
        var cellY = new IntArrayList();
        var cellZ = new IntArrayList();
        Object2LongMap<String> blockCounts = new Object2LongOpenHashMap<>();
        long[] blockCount = new long[1];

        PolarHelper.forEachSection(world, (sectionX, sectionY, sectionZ, section) -> {
            String[] palette = section.blockPalette();

            // A uniform section has no block data at all, so it cannot go through the decode below.
            if (palette.length == 1) {
                blockCount[0] += PolarSection.BLOCK_PALETTE_SIZE;
                blockCounts.mergeLong(PolarHelper.blockId(palette[0]), PolarSection.BLOCK_PALETTE_SIZE, Long::sum);
                for (int cell = 0; cell < CELLS_PER_SECTION; cell++)
                    addCell(cells, cellX, cellY, cellZ, sectionX, sectionY, sectionZ, cell);
                return;
            }

            boolean[] air = new boolean[palette.length];
            for (int i = 0; i < palette.length; i++) air[i] = PolarHelper.isAir(palette[i]);

            // Counting into a local array first keeps the hot loop off the hash map, which matters
            // on terrain maps where a single world runs to tens of millions of blocks.
            long[] counts = new long[palette.length];
            boolean[] occupied = new boolean[CELLS_PER_SECTION];
            int[] data = section.blockData();
            for (int i = 0; i < PolarSection.BLOCK_PALETTE_SIZE; i++) {
                int entry = data[i];
                if (air[entry]) continue;

                counts[entry]++;
                occupied[cellOf(i)] = true;
            }

            for (int i = 0; i < palette.length; i++) {
                if (counts[i] == 0) continue;
                blockCount[0] += counts[i];
                blockCounts.mergeLong(PolarHelper.blockId(palette[i]), counts[i], Long::sum);
            }
            for (int cell = 0; cell < CELLS_PER_SECTION; cell++) {
                if (occupied[cell])
                    addCell(cells, cellX, cellY, cellZ, sectionX, sectionY, sectionZ, cell);
            }
        });

        var scan = TriggerScan.scan(world);
        var mechanics = MechanicScan.scan(scan.triggers());

        long[] counts = blockCounts.values().toLongArray();
        Arrays.sort(counts); // ascending, so the dominant block is the last entry
        int[] x = sorted(cellX);
        int[] y = sorted(cellY);
        int[] z = sorted(cellZ);

        var checkpoints = scan.triggers().stream().filter(t -> t.kind() == TriggerScan.Kind.CHECKPOINT).toList();

        // A map can be published with an empty world, which leaves nothing to measure but does not
        // stop it having triggers; every geometry field reads as zero for it.
        return new MapFeatures(
            scan.dataVersion(),
            blockCount[0],
            trimmedExtent(x), trimmedExtent(y), trimmedExtent(z),
            x.length,
            counts.length,
            blockCount[0] == 0 ? 0 : (double) counts[counts.length - 1] / blockCount[0],
            scan.entities(), scan.textDisplays(),
            checkpoints.size(),
            medianNearestNeighbour(checkpoints),
            count(scan.triggers(), TriggerScan.Kind.FINISH),
            count(scan.triggers(), TriggerScan.Kind.STATUS),
            mechanics.mechanics(),
            mechanics.attributes(),
            mechanics.potionEffects(),
            mechanics.settings(),
            mechanics.actionCount(),
            scan.decodeFailures()
        );
    }

    // Structure

    private static int count(List<TriggerScan.Trigger> triggers, TriggerScan.Kind kind) {
        return (int) triggers.stream().filter(t -> t.kind() == kind).count();
    }

    /// Median over checkpoints of the distance to the closest other checkpoint. Quadratic, but a
    /// map with a thousand checkpoints does not exist and this is cheaper than building a tree
    /// per map.
    private static double medianNearestNeighbour(List<TriggerScan.Trigger> checkpoints) {
        if (checkpoints.size() < 2) return 0;

        var nearest = new double[checkpoints.size()];
        for (int i = 0; i < checkpoints.size(); i++) {
            var a = checkpoints.get(i);
            double best = Double.MAX_VALUE;
            for (int j = 0; j < checkpoints.size(); j++) {
                if (i == j) continue;
                var b = checkpoints.get(j);
                double dx = a.x() - b.x();
                double dy = a.y() - b.y();
                double dz = a.z() - b.z();
                best = Math.min(best, dx * dx + dy * dy + dz * dz);
            }
            nearest[i] = Math.sqrt(best);
        }
        Arrays.sort(nearest);
        return nearest[nearest.length / 2];
    }

    // Cells

    /// Marks the cell a section-local cell index falls in, recording its coordinates the first
    /// time it is seen so extent can be measured without unpacking the keys again.
    private static void addCell(
        LongOpenHashSet cells, IntArrayList cellX, IntArrayList cellY, IntArrayList cellZ,
        int sectionX, int sectionY, int sectionZ, int cell
    ) {
        int x = sectionX * 2 + (cell & 1);
        int y = sectionY * 2 + ((cell >> 2) & 1);
        int z = sectionZ * 2 + ((cell >> 1) & 1);

        if (!cells.add(key(x, y, z))) return;
        cellX.add(x);
        cellY.add(y);
        cellZ.add(z);
    }

    /// Section block data is ordered `x + z * 16 + y * 256`, so the cell a block falls in is just
    /// the top bit of each coordinate.
    private static int cellOf(int blockIndex) {
        return ((blockIndex >> 3) & 1)          // x
            | (((blockIndex >> 7) & 1) << 1)    // z
            | (((blockIndex >> 11) & 1) << 2);  // y
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private static int[] sorted(IntArrayList values) {
        int[] out = values.toIntArray();
        Arrays.sort(out);
        return out;
    }

    /// Extent along one axis in blocks, discarding the outermost [#EXTENT_TRIM_FACTOR] of cells at each end.
    private static int trimmedExtent(int[] sortedCells) {
        if (sortedCells.length == 0) return 0;
        int trim = (int) (sortedCells.length * EXTENT_TRIM_FACTOR);
        return (sortedCells[sortedCells.length - 1 - trim] - sortedCells[trim] + 1) * CELL_SIZE;
    }

    private MapIndexer() {
    }
}
