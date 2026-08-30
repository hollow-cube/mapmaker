package net.hollowcube.proxy.anticheat;

import net.hollowcube.anticheat.capture.CaptureEngineConfig;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/// Anticheat capture settings, entirely from the environment (the proxy has no config file of its
/// own for us). Nothing here throws: a variable that does not parse or is out of range is logged
/// and the default is used, because a typo in a deployment must not take the proxy down.
///
/// @param enabled       `ANTICHEAT_ENABLED`, whether the tap is installed at all. Off by default;
///                      the metrics are served on ProxyHttpServer regardless, so a proxy can be
///                      scraped before any capture happens.
/// @param spoolDir      `ANTICHEAT_SPOOL_DIR`, per-connection spool files live in subdirs of this.
/// @param ringWindow    `ANTICHEAT_RING_SECONDS`, how far back a ring flush reaches. The effective
///                      window is this plus up to a snapshot interval, see plan 1.6.
/// @param ringMaxBytes  `ANTICHEAT_RING_MAX_BYTES`, per-connection cap on ring frames.
/// @param spoolMaxBytes `ANTICHEAT_SPOOL_MAX_BYTES`, cap on the whole spool dir; new captures are
///                      refused (and counted) once it is hit.
/// @param shutdownGrace `ANTICHEAT_SHUTDOWN_SECONDS`, how long a proxy shutdown waits for the last
///                      traces to reach the store before it stops waiting.
public record AnticheatConfig(
    boolean enabled,
    Path spoolDir,
    Duration ringWindow,
    long ringMaxBytes,
    long spoolMaxBytes,
    Duration shutdownGrace
) {
    /// The ring, spool and shutdown defaults are the engine's own, since every one of them is
    /// handed straight to a [CaptureEngineConfig]; only the env can make them differ.
    public static final AnticheatConfig DEFAULT = new AnticheatConfig(
        false,
        Path.of("/tmp/anticheat"),
        Duration.ofNanos(CaptureEngineConfig.RING_WINDOW_NS),
        CaptureEngineConfig.RING_MAX_BYTES,
        CaptureEngineConfig.MAX_SPOOL_BYTES,
        CaptureEngineConfig.CLOSE_TIMEOUT
    );

    public static AnticheatConfig fromEnv(Logger logger) {
        return from(System.getenv(), logger);
    }

    public static AnticheatConfig from(Map<String, String> env, Logger logger) {
        var reader = new Reader(env, logger);
        return new AnticheatConfig(
            reader.read("ANTICHEAT_ENABLED", DEFAULT.enabled, AnticheatConfig::parseBoolean),
            reader.read("ANTICHEAT_SPOOL_DIR", DEFAULT.spoolDir, Path::of),
            reader.read("ANTICHEAT_RING_SECONDS", DEFAULT.ringWindow, AnticheatConfig::parseSeconds),
            reader.read("ANTICHEAT_RING_MAX_BYTES", DEFAULT.ringMaxBytes, AnticheatConfig::parsePositiveLong),
            reader.read("ANTICHEAT_SPOOL_MAX_BYTES", DEFAULT.spoolMaxBytes, AnticheatConfig::parsePositiveLong),
            reader.read("ANTICHEAT_SHUTDOWN_SECONDS", DEFAULT.shutdownGrace, AnticheatConfig::parseSeconds)
        );
    }

    /// Where the capture engines leave assembled traces and the shipper picks them up.
    public Path tracesDir() {
        return spoolDir.resolve("traces");
    }

    /// Where a trace the store refused outright is kept, out of the sweeper's way, for whoever
    /// wants to know why it was refused.
    public Path failedDir() {
        return spoolDir.resolve("failed");
    }

    private record Reader(Map<String, String> env, Logger logger) {
        <T> T read(String name, T fallback, Function<String, T> parse) {
            var raw = env.get(name);
            if (raw == null || raw.isBlank()) return fallback;
            try {
                return parse.apply(raw.trim());
            } catch (RuntimeException e) {
                logger.warn("anticheat: ignoring {}={} ({}), using {}", name, raw, e.getMessage(), fallback);
                return fallback;
            }
        }
    }

    private static boolean parseBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> throw new IllegalArgumentException("not a boolean");
        };
    }


    private static Duration parseSeconds(String value) {
        return Duration.ofSeconds(parsePositiveLong(value));
    }

    private static long parsePositiveLong(String value) {
        var parsed = Long.parseLong(value);
        if (parsed <= 0) throw new IllegalArgumentException("must be positive");
        return parsed;
    }
}
