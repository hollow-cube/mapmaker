package net.hollowcube.proxy.anticheat;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

import java.util.Locale;

/// Every anticheat metric the proxy exports, on the same simpleclient the backend servers use and
/// registered on the default registry the way theirs are; [net.hollowcube.proxy.ProxyHttpServer]
/// serves it on `/metrics` exactly as `MapServerInitializer` does. Names and labels are the capture
/// plan's section 6 — the scrape config in hollow-cube/velocity-proxy and any dashboard are written
/// against these, so they only change together.
///
/// Static because there is exactly one of each per process, and threading an instance through the
/// tap, the connections and the shipper bought nothing; tests `clear()` what they assert on.
public final class AnticheatMetrics {
    /// Direction label, from the client's point of view: c2s is what the player sent.
    public enum Dir {
        C2S, S2C;

        public final String label = name().toLowerCase(Locale.ROOT);
    }

    /// Why a connection, capture or frame was not recorded.
    public enum Drop {
        /// Client protocol version we have no packet registry for; the tap is not installed.
        UNSUPPORTED_PVN,
        /// Total spool cap hit, so the capture was refused.
        SPOOL_CAP,
        /// Per-connection ring cap hit, so the oldest frames were dropped.
        RING_CAP,
        /// Tap installed mid-state, so frames are discarded until the next state boundary.
        UNKNOWN_STATE,
        /// Capture shorter than the floor, thrown away rather than assembled.
        TOO_SHORT;

        public final String label = name().toLowerCase(Locale.ROOT);
    }

    /// Connections currently open, by client protocol version and whether the tap is installed.
    public static final Gauge connections = Gauge.build()
        .name("anticheat_connections")
        .help("Open player connections")
        .labelNames("pvn", "tapped")
        .register();

    /// Captures currently recording to a spool file.
    public static final Gauge capturesActive = Gauge.build()
        .name("anticheat_captures_active")
        .help("Captures currently recording")
        .register();

    public static final Counter frames = Counter.build()
        .name("anticheat_frames_total")
        .help("Frames kept by the tap")
        .labelNames("dir")
        .register();

    public static final Counter bytes = Counter.build()
        .name("anticheat_bytes_total")
        .help("Bytes of kept frames")
        .labelNames("dir")
        .register();

    /// Pings the tap injected on flush, in the proxy's negative id space.
    public static final Counter pings = Counter.build()
        .name("anticheat_pings_total")
        .help("Pings injected by the tap")
        .register();

    /// Pongs to injected pings, swallowed rather than forwarded to the backend.
    public static final Counter pongsSwallowed = Counter.build()
        .name("anticheat_pongs_swallowed_total")
        .help("Pongs to injected pings, not forwarded")
        .register();

    /// Ring frame bytes summed over every connection.
    public static final Gauge ringBytes = Gauge.build()
        .name("anticheat_ring_bytes")
        .help("Ring buffer bytes over all connections")
        .register();

    public static final Counter traces = Counter.build()
        .name("anticheat_traces_total")
        .help("Traces assembled and shipped")
        .labelNames("reason", "closedBy", "result")
        .register();

    public static final Histogram traceBytes = Histogram.build()
        .name("anticheat_trace_bytes")
        .help("Size of assembled traces")
        .buckets(64 * 1024, 256 * 1024, 1024 * 1024, 4 * 1024 * 1024,
            16 * 1024 * 1024, 64 * 1024 * 1024, 256 * 1024 * 1024)
        .register();

    public static final Histogram shipDuration = Histogram.build()
        .name("anticheat_ship_duration_seconds")
        .help("Time to PUT a trace to the store")
        .buckets(0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10, 30, 60)
        .register();

    /// Bytes on disk in the spool dir.
    public static final Gauge spoolBytes = Gauge.build()
        .name("anticheat_spool_bytes")
        .help("Bytes currently spooled to disk")
        .register();

    public static final Counter dropped = Counter.build()
        .name("anticheat_dropped_total")
        .help("Connections, captures and frames not recorded")
        .labelNames("cause")
        .register();

    public static void dropped(Drop cause) {
        dropped.labels(cause.label).inc();
    }

    /// The same, for a cause counted in arrears rather than one at a time.
    public static void dropped(Drop cause, double count) {
        dropped.labels(cause.label).inc(count);
    }

    private AnticheatMetrics() {}
}
