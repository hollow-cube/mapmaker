package net.hollowcube.proxy.anticheat;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnticheatMetricsTest {

    /// Plan section 6, verbatim, as the sample names a scrape carries. Changing this set means
    /// changing the proxy scrape config too.
    private static final Set<String> EXPECTED = Set.of(
            "anticheat_connections",
            "anticheat_captures_active",
            "anticheat_frames_total",
            "anticheat_bytes_total",
            "anticheat_pings_total",
            "anticheat_pongs_swallowed_total",
            "anticheat_ring_bytes",
            "anticheat_traces_total",
            "anticheat_trace_bytes_bucket",
            "anticheat_ship_duration_seconds_bucket",
            "anticheat_spool_bytes",
            "anticheat_dropped_total"
    );

    @Test
    void testScrapeCarriesEveryPlannedMetric() throws IOException {
        touchAll();

        var body = scrape();
        for (var name : EXPECTED)
            assertTrue(body.contains(name + "{") || body.contains(name + " "), name + " missing from scrape");
    }

    @Test
    void testLabelNames() throws IOException {
        touchAll();

        var body = scrape();
        assertTrue(body.contains("anticheat_connections{pvn=\"776\",tapped=\"true\",}"), body);
        assertTrue(body.contains("anticheat_frames_total{dir=\"c2s\",}"), body);
        assertTrue(body.contains("anticheat_bytes_total{dir=\"s2c\",}"), body);
        assertTrue(body.contains("anticheat_traces_total{reason=\"run\",closedBy=\"stop\",result=\"ok\",}"), body);
        assertTrue(body.contains("anticheat_dropped_total{cause=\"unsupported_pvn\",}"), body);
    }

    /// Every metric needs a data point before it appears on the wire.
    private static void touchAll() {
        AnticheatMetrics.connections.labels("776", "true").inc();
        AnticheatMetrics.capturesActive.inc();
        for (var dir : AnticheatMetrics.Dir.values()) {
            AnticheatMetrics.frames.labels(dir.label).inc();
            AnticheatMetrics.bytes.labels(dir.label).inc(1024);
        }
        AnticheatMetrics.pings.inc();
        AnticheatMetrics.pongsSwallowed.inc();
        AnticheatMetrics.ringBytes.set(4096);
        AnticheatMetrics.traces.labels("run", "stop", "ok").inc();
        AnticheatMetrics.traceBytes.observe(1234);
        AnticheatMetrics.shipDuration.observe(0.5);
        AnticheatMetrics.spoolBytes.set(8192);
        for (var cause : AnticheatMetrics.Drop.values()) AnticheatMetrics.dropped(cause);
    }

    /// The statics register on the default registry, which is exactly what the proxy serves.
    private static String scrape() throws IOException {
        var writer = new StringWriter();
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
        return writer.toString();
    }
}
