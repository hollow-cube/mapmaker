package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.Frame;
import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.ProtocolState;
import net.hollowcube.anticheat.state.StateCache;
import net.hollowcube.anticheat.world.ChunkMap;
import org.junit.jupiter.api.Test;

import static net.hollowcube.anticheat.capture.TestCapture.SECOND;
import static org.junit.jupiter.api.Assertions.*;

/// The ring's window arithmetic, which is the whole reason a flush can promise sixty seconds when
/// snapshots only happen every thirty.
class RingBufferTest {

    private static final long WINDOW = 60 * SECOND;
    private static final long INTERVAL = 30 * SECOND;

    @Test
    void testKeepsOneSnapshotAtOrBeforeTheWindowStart() {
        var ring = new RingBuffer(WINDOW, INTERVAL, 1 << 20);

        ring.snapshot(snapshot(0));
        assertEquals(1, ring.snapshotCount());
        assertEquals(0, ring.oldestSnapshotNs());

        ring.snapshot(snapshot(30 * SECOND));
        assertEquals(2, ring.snapshotCount());
        assertEquals(0, ring.oldestSnapshotNs());

        // 0 is exactly the window start of 60, and the window has to be covered, so it stays.
        ring.snapshot(snapshot(60 * SECOND));
        assertEquals(3, ring.snapshotCount());
        assertEquals(0, ring.oldestSnapshotNs());

        // At 90 the window starts at 30, which 30 covers on its own; 0 is now unreachable.
        ring.snapshot(snapshot(90 * SECOND));
        assertEquals(3, ring.snapshotCount());
        assertEquals(30 * SECOND, ring.oldestSnapshotNs());

        ring.snapshot(snapshot(120 * SECOND));
        assertEquals(3, ring.snapshotCount());
        assertEquals(60 * SECOND, ring.oldestSnapshotNs());
    }

    @Test
    void testFlushAfterNinetySecondsStartsFromTheOlderSnapshot() {
        var ring = new RingBuffer(WINDOW, INTERVAL, 1 << 20);
        fill(ring, 95);

        var flush = ring.flush();
        assertNotNull(flush);
        assertEquals(30 * SECOND, flush.snapshot().tNs());
        assertEquals(30 * SECOND, flush.frames().getFirst().tNs());
        assertEquals(95 * SECOND, flush.frames().getLast().tNs());
        assertEquals(66, flush.frames().size());
        assertFalse(flush.truncated());
        // Which is more than the window promises, and never less.
        assertTrue(flush.frames().getLast().tNs() - flush.snapshot().tNs() >= WINDOW);
    }

    @Test
    void testTheFlushAlwaysCoversTheWindow() {
        var ring = new RingBuffer(WINDOW, INTERVAL, 1 << 20);
        for (int second = 0; second <= 300; second++) {
            step(ring, second);
            var flush = ring.flush();
            assertNotNull(flush);
            long covered = second * SECOND - flush.snapshot().tNs();
            assertTrue(covered >= Math.min(second * SECOND, WINDOW), "only covered " + covered + " at " + second);
            assertTrue(covered <= WINDOW + INTERVAL, "covered " + covered + " at " + second);
        }
    }

    @Test
    void testFramesBeforeTheOldestSnapshotAreDroppedWithoutTruncation() {
        var ring = new RingBuffer(WINDOW, INTERVAL, 1 << 20);
        fill(ring, 95);

        assertEquals(66, ring.frameCount());
        assertFalse(ring.truncated(), "dropping unreachable frames is not truncation");
    }

    @Test
    void testTheByteCapEvictsTheOldestFramesAndFlagsTruncation() {
        // Room for ten frames of a hundred payload bytes plus their overhead.
        var ring = new RingBuffer(WINDOW, INTERVAL, 10L * (100 + RingBuffer.FRAME_OVERHEAD_BYTES));
        ring.snapshot(snapshot(0));
        for (int second = 0; second <= 20; second++) ring.frame(frame(second * SECOND, 100));

        assertTrue(ring.truncated());
        assertEquals(10, ring.frameCount());
        assertTrue(ring.bytes() <= 10L * (100 + RingBuffer.FRAME_OVERHEAD_BYTES));

        var flush = ring.flush();
        assertNotNull(flush);
        assertTrue(flush.truncated());
        assertEquals(11 * SECOND, flush.frames().getFirst().tNs());
    }

    @Test
    void testTheByteCapDropsSnapshotsItHasCutInto() {
        var ring = new RingBuffer(WINDOW, INTERVAL, 4L * (100 + RingBuffer.FRAME_OVERHEAD_BYTES));
        for (int second = 0; second <= 95; second++) {
            if (ring.wantsSnapshot(second * SECOND)) ring.snapshot(snapshot(second * SECOND));
            ring.frame(frame(second * SECOND, 100));
        }

        // Everything up to 91 has been evicted, so the 30 and 60 second snapshots can no longer
        // start a trace and only the newest one is left.
        assertEquals(1, ring.snapshotCount());
        assertEquals(90 * SECOND, ring.oldestSnapshotNs());
        assertTrue(ring.truncated());
    }

    @Test
    void testThereIsNothingToFlushBeforeTheFirstSnapshot() {
        var ring = new RingBuffer(WINDOW, INTERVAL, 1 << 20);
        ring.frame(frame(0, 10));

        assertNull(ring.flush());
        assertEquals(Long.MIN_VALUE, ring.oldestSnapshotNs());
    }

    private static void fill(RingBuffer ring, int seconds) {
        for (int second = 0; second <= seconds; second++) step(ring, second);
    }

    private static void step(RingBuffer ring, int second) {
        if (ring.wantsSnapshot(second * SECOND)) ring.snapshot(snapshot(second * SECOND));
        ring.frame(frame(second * SECOND, 8));
    }

    private static Snapshot snapshot(long tNs) {
        return Snapshot.of(tNs, Frame.NO_PING, new ChunkMap(), new StateCache());
    }

    private static Frame frame(long tNs, int size) {
        return new Frame(tNs, Direction.S2C, ProtocolState.PLAY, 1, Frame.NO_PING, new byte[size]);
    }
}
