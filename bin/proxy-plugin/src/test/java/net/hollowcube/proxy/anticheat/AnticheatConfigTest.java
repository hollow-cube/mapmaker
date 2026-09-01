package net.hollowcube.proxy.anticheat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnticheatConfigTest {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AnticheatConfigTest.class);

    @Test
    void testDefaults() {
        var config = AnticheatConfig.from(Map.of(), logger);

        assertEquals(AnticheatConfig.DEFAULT, config);
        assertFalse(config.enabled());
        assertEquals(Path.of("/tmp/anticheat"), config.spoolDir());
        assertEquals(Duration.ofSeconds(60), config.ringWindow());
        assertEquals(8 * 1024 * 1024, config.ringMaxBytes());
        assertEquals(256L * 1024 * 1024, config.spoolMaxBytes());
        assertEquals(Duration.ofSeconds(3), config.minCapture());
    }

    @Test
    void testOverrides() {
        var config = AnticheatConfig.from(Map.of(
                "ANTICHEAT_ENABLED", "true",
                "ANTICHEAT_SPOOL_DIR", "/var/anticheat",
                "ANTICHEAT_RING_SECONDS", "90",
                "ANTICHEAT_RING_MAX_BYTES", "1024",
                "ANTICHEAT_SPOOL_MAX_BYTES", "2048",
                "ANTICHEAT_MIN_CAPTURE_SECONDS", "10"
        ), logger);

        assertTrue(config.enabled());
        assertEquals(Path.of("/var/anticheat"), config.spoolDir());
        assertEquals(Duration.ofSeconds(90), config.ringWindow());
        assertEquals(1024, config.ringMaxBytes());
        assertEquals(2048, config.spoolMaxBytes());
        assertEquals(Duration.ofSeconds(10), config.minCapture());
    }

    @Test
    void testBlankIsDefault() {
        var config = AnticheatConfig.from(Map.of(
                "ANTICHEAT_ENABLED", "",
                "ANTICHEAT_RING_SECONDS", "   "
        ), logger);

        assertEquals(AnticheatConfig.DEFAULT, config);
    }

    @Test
    void testInvalidFallsBackToDefaults() {
        var config = AnticheatConfig.from(Map.of(
                "ANTICHEAT_ENABLED", "maybe",
                "ANTICHEAT_RING_SECONDS", "0",
                "ANTICHEAT_RING_MAX_BYTES", "-1",
                "ANTICHEAT_SPOOL_MAX_BYTES", "lots",
                "ANTICHEAT_SHUTDOWN_SECONDS", "later"
        ), logger);

        assertEquals(AnticheatConfig.DEFAULT, config);
    }

    @Test
    void testBooleanSpellings() {
        for (var yes : new String[]{"true", "TRUE", " yes ", "1", "on"})
            assertTrue(AnticheatConfig.from(Map.of("ANTICHEAT_ENABLED", yes), logger).enabled(), yes);
        for (var no : new String[]{"false", "No", "0", "off"})
            assertFalse(AnticheatConfig.from(Map.of("ANTICHEAT_ENABLED", no), logger).enabled(), no);
    }
}
