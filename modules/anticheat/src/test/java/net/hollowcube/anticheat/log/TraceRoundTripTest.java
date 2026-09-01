package net.hollowcube.anticheat.log;

import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.ProtocolState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceRoundTripTest {

    @TempDir
    Path directory;

    @Test
    void testRoundTrip() {
        var path = directory.resolve("round-trip.trace");
        var written = TraceFixture.write(path);

        var trace = TraceReader.read(path);
        assertFalse(trace.truncated());
        assertEquals(written, trace.header());
        TraceFixture.assertMatches(trace);
    }

    @Test
    void testWriterCountsWhatItWrote() {
        var path = directory.resolve("counters.trace");
        var counters = TraceFixture.write(path).counters();

        assertEquals(10, counters.frames());
        assertEquals(TraceFixture.totalFrameBytes(), counters.bytes());
        assertEquals(2, counters.preludeFrames());
        assertEquals(2, counters.chunks());
        assertEquals(17, counters.droppedFrames(), "the tap's dropped count survives assembly");
    }

    @Test
    void testHeaderReadsWithoutTheBody() {
        var path = directory.resolve("header-only.trace");
        var written = TraceFixture.write(path);

        assertEquals(written, TraceReader.header(path));
    }

    @Test
    void testHeaderJsonUsesTheNamesTheBackendSpeaks() {
        var json = TraceFixture.header().toJson();

        assertTrue(json.contains("\"reason\":\"run\""), json);
        assertTrue(json.contains("\"closedBy\":\"stop\""), json);
        assertTrue(json.contains("\"cohort\":\"trusted\""), json);
        assertTrue(json.contains("\"startedAt\":\"2026-08-29T12:00:00Z\""), json);
    }

    @Test
    void testHeaderIgnoresUnknownFieldsAndDefaultsMissingOnes() {
        var header = TraceHeader.fromJson("{\"clientPvn\":776,\"somethingNewer\":{\"a\":1}}");

        assertEquals(776, header.clientPvn());
        assertEquals(TraceHeader.Flags.NONE, header.flags());
        assertEquals(TraceHeader.Counters.EMPTY, header.counters());
        assertTrue(header.extras().isEmpty());
    }

    /// A trace whose header grew past the region reserved for it would silently lose its tail,
    /// so it fails loudly instead. Only the close-time fields are supposed to change.
    @Test
    void testHeaderThatOutgrowsItsRegionFails() {
        var path = directory.resolve("outgrown.trace");
        var writer = TraceWriter.open(path, TraceFixture.header(), List.of(), TraceWorld.EMPTY);
        writer.header(bloated());

        assertThrows(IllegalStateException.class, writer::finish);
    }

    @Test
    void testTruncatedBodyKeepsEveryCompleteFrame() throws IOException {
        var path = directory.resolve("truncated.trace");
        var frames = TraceFixture.frames();
        try (TraceWriter writer = TraceWriter.open(path, TraceFixture.header(), TraceFixture.prelude(),
            TraceFixture::chunks)) {
            for (var frame : frames) {
                writer.frame(frame);
                writer.flush(); // a crashed capture is only readable up to its last flush
            }
        }

        byte[] whole = Files.readAllBytes(path);
        // Cuts inside the reserved header region are a different failure: without a header there is
        // nothing to interpret, so the reader refuses those rather than returning a trace.
        int bodyStart = TraceFormat.FIXED_HEAD_LENGTH + ByteBuffer.wrap(whole).getInt(6);
        var recovered = new ArrayList<Integer>();
        for (int i = 1; i < 32; i++) {
            int length = whole.length * i / 32;
            if (length <= bodyStart) continue;
            var cut = directory.resolve("cut-" + length + ".trace");
            Files.write(cut, Arrays.copyOf(whole, length));

            var trace = TraceReader.read(cut);
            assertTrue(trace.truncated(), "a cut trace is truncated at " + length);
            assertEquals(TraceFixture.header().withCounters(trace.header().counters()), trace.header(),
                "the header survives a cut body");
            TraceFixture.assertFramesEqual(frames.subList(0, trace.frames().size()), trace.frames());
            recovered.add(trace.frames().size());
        }
        assertTrue(recovered.stream().anyMatch(count -> count > 0),
            "cutting the tail off should still recover the frames before it, got " + recovered);
    }

    @Test
    void testTruncatedWorldSectionYieldsNoFrames() throws IOException {
        var path = directory.resolve("short.trace");
        TraceFixture.write(path);
        byte[] whole = Files.readAllBytes(path);
        var cut = directory.resolve("short-cut.trace");
        int bodyStart = TraceFormat.FIXED_HEAD_LENGTH + ByteBuffer.wrap(whole).getInt(6);
        Files.write(cut, Arrays.copyOf(whole, bodyStart + 8));

        var trace = TraceReader.read(cut);
        assertTrue(trace.truncated());
        assertTrue(trace.frames().isEmpty());
    }

    @Test
    void testUnknownFormatVersionIsRefused() throws IOException {
        var path = directory.resolve("from-the-future.trace");
        TraceFixture.write(path);
        int newer = TraceFormat.VERSION_LATEST + 1;
        byte[] whole = Files.readAllBytes(path);
        whole[4] = (byte) (newer >> 8);
        whole[5] = (byte) newer;
        Files.write(path, whole);

        var error = assertThrows(TraceFormatException.class, () -> TraceReader.read(path));
        assertTrue(error.getMessage().contains("unsupported trace format version " + newer), error.getMessage());
    }

    @Test
    void testWriterRefusesAHeaderFromAnotherVersion() {
        var header = TraceHeader.fromJson("{\"formatVersion\":" + (TraceFormat.VERSION_LATEST - 1) + "}");

        assertThrows(IllegalArgumentException.class, () -> TraceWriter.open(
            directory.resolve("wrong-version.trace"), header, List.of(), TraceWorld.EMPTY));
    }

    @Test
    void testNotATraceFile() throws IOException {
        var path = directory.resolve("nope.trace");
        Files.write(path, new byte[]{'N', 'O', 'P', 'E', 0, 1, 0, 0, 0, 0, 0, 0, 0, 0});

        var error = assertThrows(TraceFormatException.class, () -> TraceReader.read(path));
        assertTrue(error.getMessage().contains("not a trace file"), error.getMessage());
    }

    @Test
    void testEmptyTrace() {
        var path = directory.resolve("empty.trace");
        TraceWriter.assemble(path, TraceFixture.header(), List.of(), TraceWorld.EMPTY, FrameSource.EMPTY);

        var trace = TraceReader.read(path);
        assertFalse(trace.truncated());
        assertTrue(trace.prelude().isEmpty());
        assertTrue(trace.chunks().isEmpty());
        assertTrue(trace.frames().isEmpty());
    }

    @Test
    void testFrameLayout() {
        var path = directory.resolve("layout.trace");
        Frame frame = new Frame(1234567890123L, Direction.C2S, ProtocolState.PLAY, 0x1D, 3,
            new byte[]{1, 2, 3});
        TraceWriter.assemble(path, TraceFixture.header(), List.of(), TraceWorld.EMPTY,
            FrameSource.of(List.of(frame)));

        TraceFixture.assertFramesEqual(List.of(frame), TraceReader.read(path).frames());
    }

    private static TraceHeader bloated() {
        var filler = new StringBuilder();
        while (filler.length() < TraceFormat.HEADER_SLACK * 2) filler.append("filler");
        return new TraceHeader(TraceFormat.VERSION_LATEST, TraceDictionary.LATEST, 776, filler.toString(), null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null);
    }
}
