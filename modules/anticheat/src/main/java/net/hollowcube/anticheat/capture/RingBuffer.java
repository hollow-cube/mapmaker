package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.Frame;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/// The always-on tail of a connection: the frames of the last window, and the periodic snapshots
/// one of them can be replayed from.
///
/// A flush has to be able to start far enough back to cover the whole window, and a snapshot only
/// exists every `snapshotInterval`, so the ring keeps one snapshot **at or before** the window
/// start and drops everything older. With the plan's numbers (60 s window, 30 s interval) that is
/// three snapshots and 60–90 s of frames, which is where the "effective window of 60–90 s" comes
/// from — a flush uses the oldest snapshot and every frame since.
///
/// Frames before the oldest snapshot are dropped as unreachable, not as loss: no trace could have
/// started there. Losing frames to [#maxBytes] is loss, and sets [#truncated()] for the header.
///
/// Owned by the connection's event loop, like everything else in the model.
public final class RingBuffer {

    /// Charged per frame on top of its payload, for the object, its header and the deque slot. The
    /// cap is a memory bound, and a flood of empty frames is exactly the case a payload-only count
    /// would miss.
    public static final int FRAME_OVERHEAD_BYTES = 32;

    /// What a flush found: a snapshot and every frame the ring kept after it.
    public record Flush(Snapshot snapshot, List<Frame> frames, boolean truncated) {

        public Flush {
            frames = List.copyOf(frames);
        }
    }

    private final long windowNs;
    private final long snapshotIntervalNs;
    private final long maxBytes;

    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private final List<Snapshot> snapshots = new ArrayList<>();
    /// Written only by the event loop, but read by whoever is sampling metrics, hence volatile;
    /// everything else in here is the event loop's alone.
    private volatile long bytes;
    private volatile long evictedFrames;
    private long lastSnapshotNs;
    private long evictedThroughNs = Long.MIN_VALUE;
    private boolean truncated;

    /// The three numbers are [CaptureEngineConfig]'s, which is where they are checked.
    public RingBuffer(long windowNs, long snapshotIntervalNs, long maxBytes) {
        this.windowNs = windowNs;
        this.snapshotIntervalNs = snapshotIntervalNs;
        this.maxBytes = maxBytes;
    }

    /// Whether the interval has elapsed and the caller should hand over a fresh [Snapshot]. Asked
    /// before the frame at `tNs` is applied, so the snapshot and the frames after it line up.
    public boolean wantsSnapshot(long tNs) {
        return snapshots.isEmpty() || tNs - lastSnapshotNs >= snapshotIntervalNs;
    }

    public void snapshot(Snapshot snapshot) {
        snapshots.add(snapshot);
        lastSnapshotNs = snapshot.tNs();
        // Keep exactly one snapshot at or before the window start; an older one can only produce a
        // trace the newer one already covers.
        while (snapshots.size() > 1 && snapshots.get(1).tNs() <= snapshot.tNs() - windowNs)
            snapshots.removeFirst();
        dropUnreachable();
    }

    public void frame(Frame frame) {
        frames.addLast(frame);
        bytes += size(frame);
        if (bytes > maxBytes) evictBytes();
    }

    /// The oldest snapshot and everything after it, or null before the first snapshot.
    public @Nullable Flush flush() {
        if (snapshots.isEmpty()) return null;
        return new Flush(snapshots.getFirst(), List.copyOf(frames), truncated);
    }

    /// The time the next trace out of this ring would start at, or [Long#MIN_VALUE] when there is
    /// no snapshot to start from.
    public long oldestSnapshotNs() {
        return snapshots.isEmpty() ? Long.MIN_VALUE : snapshots.getFirst().tNs();
    }

    public int snapshotCount() {
        return snapshots.size();
    }

    public int frameCount() {
        return frames.size();
    }

    public long bytes() {
        return bytes;
    }

    /// Frames the byte cap has cost this ring, which is the loss [#truncated()] stands for.
    public long evictedFrames() {
        return evictedFrames;
    }

    /// Whether the byte cap has cost this ring frames a flush would otherwise have carried.
    public boolean truncated() {
        return truncated;
    }

    /// The cap has been hit: drop the oldest frames, and with them any snapshot they were the
    /// beginning of, because a trace starting there would now be missing what came next.
    private void evictBytes() {
        while (bytes > maxBytes && frames.size() > 1) {
            var evicted = frames.removeFirst();
            bytes -= size(evicted);
            evictedFrames++;
            evictedThroughNs = evicted.tNs();
        }
        truncated = true;
        while (snapshots.size() > 1 && snapshots.getFirst().tNs() <= evictedThroughNs)
            snapshots.removeFirst();
        dropUnreachable();
    }

    private void dropUnreachable() {
        if (snapshots.isEmpty()) return;
        long oldest = snapshots.getFirst().tNs();
        while (!frames.isEmpty() && frames.getFirst().tNs() < oldest)
            bytes -= size(frames.removeFirst());
    }

    private static int size(Frame frame) {
        return frame.bytes().length + FRAME_OVERHEAD_BYTES;
    }
}
