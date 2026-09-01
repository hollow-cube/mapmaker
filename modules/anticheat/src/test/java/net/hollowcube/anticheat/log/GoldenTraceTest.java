package net.hollowcube.anticheat.log;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Reads the trace checked in at `src/test/resources/traces/v2`, which is how a later format
/// version proves it can still read the ones before it.
///
/// The assertion is on the decoded structures, not on the file's bytes: zstd is free to change its
/// output between versions, and what the format promises is what comes back out.
class GoldenTraceTest {

    /// Derived from the version so a bump only means running the writer below, not editing paths.
    private static final Path FIXTURE = Path.of(
        "src/test/resources/traces/v" + TraceFormat.VERSION_LATEST + "/basic.trace");

    @Test
    void testReadsTheV2Fixture() {
        var trace = TraceReader.read(resource("/traces/v2/basic.trace"));

        assertFalse(trace.truncated());
        TraceFixture.assertMatches(trace, 2, TraceDictionary.NONE);
    }

    /// The current version read back by the current reader, dictionary and all.
    @Test
    void testReadsTheLatestFixture() {
        var trace = TraceReader.read(resource("/traces/v" + TraceFormat.VERSION_LATEST + "/basic.trace"));

        assertFalse(trace.truncated());
        TraceFixture.assertMatches(trace);
        assertEquals(TraceDictionary.LATEST, trace.header().dictionaryId());
    }

    @Test
    void testDumpRunsOverTheV2Fixture() {
        Dump.main(new String[]{resource("/traces/v2/basic.trace").toString(), "--frames"});
    }

    @Test
    void testDumpPrintsTheHeaderAndTheCounts() {
        var out = new StringBuilder();
        Dump.dump(resource("/traces/v2/basic.trace"), true, out);

        var text = out.toString();
        assertTrue(text.contains("notmatt"), text);
        assertTrue(text.contains("move_player_pos"), text);
        assertTrue(text.contains("chunks          2"), text);
        assertTrue(text.contains("frames          10"), text);
    }

    /// Rewrites the checked-in fixture. Off by default — it writes into the source tree — and run
    /// with `ANTICHEAT_WRITE_FIXTURES=1 ./gradlew :modules:anticheat:test`, which is only ever
    /// wanted when the format version has just been bumped.
    @Test
    void testWriteFixture() throws IOException {
        if (System.getenv("ANTICHEAT_WRITE_FIXTURES") == null) return;
        Files.createDirectories(FIXTURE.getParent());
        TraceFixture.write(FIXTURE);
    }

    private Path resource(String name) {
        try {
            return Path.of(Objects.requireNonNull(getClass().getResource(name), name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
