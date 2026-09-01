package net.hollowcube.anticheat.capture;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/// The knobs of one connection's capture engine, with the plan's defaults.
///
/// Every duration is nanoseconds, the unit frames are timestamped in, so nothing on the hot path
/// converts.
public record CaptureEngineConfig(
    /// Where open captures spool their frames. One file per capture, deleted once assembled.
    Path spoolDir,
    /// Where finished traces land, written to a temp name and renamed.
    Path outputDir,
    long ringWindowNs,
    long ringSnapshotIntervalNs,
    long ringMaxBytes,
    long maxSpoolBytes,
    /// How long a capture may run before it is stopped and marked truncated.
    long maxCaptureNs,
    /// Frames that may be waiting on the writer thread before new ones are dropped and counted.
    int queueSize,
    /// The trim used for ring flushes, and the range entity proximity is noted at while idle.
    TrimPolicy trim,
    /// How long [CaptureEngine#close()] waits for the writer to finish what it was handed.
    Duration closeTimeout
) {

    public static final long RING_WINDOW_NS = TimeUnit.SECONDS.toNanos(60);
    public static final long RING_SNAPSHOT_INTERVAL_NS = TimeUnit.SECONDS.toNanos(30);
    public static final long RING_MAX_BYTES = 8L << 20;
    public static final long MAX_SPOOL_BYTES = 256L << 20;
    public static final long MAX_CAPTURE_NS = TimeUnit.SECONDS.toNanos(600);
    public static final int QUEUE_SIZE = 8192;
    public static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(15);

    public CaptureEngineConfig {
        if (ringWindowNs <= 0) throw new IllegalArgumentException("ring window must be positive: " + ringWindowNs);
        if (ringSnapshotIntervalNs <= 0)
            throw new IllegalArgumentException("snapshot interval must be positive: " + ringSnapshotIntervalNs);
        if (ringMaxBytes <= 0) throw new IllegalArgumentException("ring cap must be positive: " + ringMaxBytes);
        if (maxSpoolBytes <= 0) throw new IllegalArgumentException("spool cap must be positive: " + maxSpoolBytes);
        if (maxCaptureNs <= 0) throw new IllegalArgumentException("capture cap must be positive: " + maxCaptureNs);
        if (queueSize <= 0) throw new IllegalArgumentException("queue size must be positive: " + queueSize);
    }

    public static CaptureEngineConfig of(Path spoolDir, Path outputDir) {
        return new CaptureEngineConfig(spoolDir, outputDir, RING_WINDOW_NS, RING_SNAPSHOT_INTERVAL_NS, RING_MAX_BYTES,
            MAX_SPOOL_BYTES, MAX_CAPTURE_NS, QUEUE_SIZE, TrimPolicy.DEFAULT, CLOSE_TIMEOUT);
    }
}
