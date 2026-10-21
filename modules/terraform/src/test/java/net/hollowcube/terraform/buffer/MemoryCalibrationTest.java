package net.hollowcube.terraform.buffer;

import net.hollowcube.terraform.util.BlockUtil;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;

import java.util.SplittableRandom;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins [EditBuffer#sizeBytes()] to the heap, not just to itself: every shape of buffer an edit can
/// produce is measured with JOL's retained-graph walk and the estimate must land within a band of it.
/// The estimate hard-codes object layouts (ours, fastutil's, and Minestom's palette internals), so a
/// layout change in any of them shows up here rather than as a silently wrong memory budget.
class MemoryCalibrationTest {
    /// The estimate may run this far under the measured size before it is lying about resident memory.
    private static final double MIN_RATIO = 0.85;
    /// And this far over before it starts refusing edits that would have fit.
    private static final double MAX_RATIO = 1.25;

    private static final int[] STATES = new int[512];

    static {
        // JOL otherwise attaches the HotSpot Serviceability Agent to this JVM, which suspends it (a hang
        // on macOS), and tries to self-attach an instrumentation agent. Plain Unsafe offsets suffice.
        System.setProperty("jol.skipHotspotSAAttach", "true");
        System.setProperty("jol.magicFieldOffset", "true");

        int i = 0;
        for (Block block : Block.values()) {
            for (Block state : block.possibleStates()) {
                if (i == STATES.length) break;
                // Plain states only: block entities carry shared Block objects JOL counts once.
                if (!BlockUtil.isPlainState(state)) continue;
                STATES[i++] = state.stateId();
            }
            if (i == STATES.length) break;
        }
    }

    @Test
    void scatterOneCellPerSection() {
        assertCalibrated(
            "scatter (1 cell/section)",
            build(b -> {
                for (int sx = 0; sx < 64; sx++)
                    for (int sz = 0; sz < 64; sz++)
                        b.setBlockState(sx * 16 + 3, 64, sz * 16 + 5, STATES[1]);
            })
        );
    }

    @Test
    void sparseFewCellsPerSection() {
        var random = new SplittableRandom(1);
        assertCalibrated(
            "sparse (40 cells/section)",
            build(b -> {
                for (int sx = 0; sx < 16; sx++)
                    for (int sz = 0; sz < 16; sz++)
                        for (int i = 0; i < 40; i++)
                            b.setBlockState(
                                sx * 16 + random.nextInt(16),
                                random.nextInt(16),
                                sz * 16 + random.nextInt(16),
                                STATES[random.nextInt(4)]
                            );
            })
        );
    }

    @Test
    void denseFewStates() {
        var random = new SplittableRandom(2);
        assertCalibrated(
            "dense (4 states)",
            build(b -> {
                for (int x = 0; x < 64; x++)
                    for (int y = 0; y < 64; y++)
                        for (int z = 0; z < 64; z++)
                            b.setBlockState(x, y, z, STATES[random.nextInt(4)]);
            })
        );
    }

    @Test
    void denseManyStates() {
        var random = new SplittableRandom(3);
        assertCalibrated(
            "dense (200 states)",
            build(b -> {
                for (int x = 0; x < 64; x++)
                    for (int y = 0; y < 32; y++)
                        for (int z = 0; z < 64; z++)
                            b.setBlockState(x, y, z, STATES[random.nextInt(200)]);
            })
        );
    }

    @Test
    void denseDirectMode() {
        var random = new SplittableRandom(4);
        assertCalibrated(
            "dense (512 states, direct)",
            build(b -> {
                for (int x = 0; x < 32; x++)
                    for (int y = 0; y < 32; y++)
                        for (int z = 0; z < 32; z++)
                            b.setBlockState(x, y, z, STATES[random.nextInt(STATES.length)]);
            })
        );
    }

    @Test
    void uniformSections() {
        assertCalibrated(
            "uniform sections",
            build(b -> {
                for (int sx = 0; sx < 32; sx++)
                    for (int sz = 0; sz < 32; sz++) b.fillSection(sx, 4, sz, STATES[1]);
            })
        );
    }

    @Test
    void biomesOverASparseEdit() {
        assertCalibrated(
            "biomes + sparse blocks",
            build(b -> {
                for (int x = 0; x < 64; x += 4)
                    for (int y = 0; y < 64; y += 4)
                        for (int z = 0; z < 64; z += 4) {
                            b.setBiome(x, y, z, 1);
                            b.setBlockState(x, y, z, STATES[1]);
                        }
            })
        );
    }

    private static EditBuffer build(Consumer<EditBuffer.Builder> fill) {
        var builder = EditBuffer.builder();
        fill.accept(builder);
        return builder.build();
    }

    private static void assertCalibrated(String shape, EditBuffer buffer) {
        long estimate = buffer.sizeBytes();
        long measured = GraphLayout.parseInstance(buffer).totalSize();
        double ratio = (double) estimate / measured;
        assertTrue(
            ratio >= MIN_RATIO && ratio <= MAX_RATIO,
            shape
                + ": sizeBytes "
                + estimate
                + " vs measured "
                + measured
                + " (ratio "
                + ratio
                + ")"
        );
    }
}
