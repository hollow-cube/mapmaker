package net.hollowcube.anticheat.log;

import net.hollowcube.anticheat.protocol.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// The one trace every test in this package writes: fixed uuids, fixed timestamps, fixed bytes,
/// so the same call produces the same trace on any machine and the checked-in v2 fixture can be
/// regenerated from it.
///
/// It deliberately contains one of everything the format can express — both section
/// discriminators, an empty section, frames in both directions and both states, a frame with no
/// ping id — so a future format version has something to prove its reader against.
final class TraceFixture {

    static final UUID PLAYER_ID = UUID.fromString("6c9a0bdb-8f3d-4a6d-9a2e-0e3f8b7a1c55");
    static final Instant STARTED_AT = Instant.parse("2026-08-29T12:00:00Z");
    static final Instant ENDED_AT = Instant.parse("2026-08-29T12:01:30Z");

    private TraceFixture() {
    }

    static TraceHeader header() {
        return new TraceHeader(
            TraceFormat.VERSION_LATEST,
            Protocol776.PROTOCOL_VERSION,
            "vanilla",
            PLAYER_ID,
            "notmatt",
            "conn-7",
            "run-42",
            TraceHeader.Reason.RUN,
            TraceHeader.ClosedBy.STOP,
            TraceHeader.Cohort.TRUSTED,
            new TraceHeader.Trim(2, 8),
            "proxy-0",
            "1.0.0-test",
            STARTED_AT,
            ENDED_AT,
            new TraceHeader.PingIdRange(1, 9),
            new TraceHeader.Flags(true, false, false, false),
            new TraceHeader.Counters(0, 0, 0, 0, 17),
            Map.of("note", "fixture")
        );
    }

    static List<Frame> prelude() {
        return List.of(
            new Frame(0, Direction.S2C, ProtocolState.CONFIGURATION,
                Protocol776.packetId(ProtocolState.CONFIGURATION, Direction.S2C, "registry_data"),
                Frame.NO_PING, bytes(0x01, 8)),
            new Frame(0, Direction.S2C, ProtocolState.PLAY,
                Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "login"),
                Frame.NO_PING, bytes(0x02, 24))
        );
    }

    static List<WorldChunk> chunks() {
        return List.of(
            new WorldChunk(4, -3, List.of(
                new WorldChunk.SectionEntry.Inline(WorldChunk.airSection()),
                new WorldChunk.SectionEntry.Inline(palettedSection()),
                new WorldChunk.SectionEntry.ByHash(bytes(0x20, TraceFormat.SECTION_HASH_LENGTH))
            )),
            new WorldChunk(5, -3, List.of(
                new WorldChunk.SectionEntry.Inline(WorldChunk.airSection())
            ))
        );
    }

    static List<Frame> frames() {
        var frames = new ArrayList<Frame>();
        for (int i = 0; i < 10; i++) {
            boolean c2s = i % 2 == 0;
            frames.add(new Frame(
                1_000_000L * i,
                c2s ? Direction.C2S : Direction.S2C,
                ProtocolState.PLAY,
                c2s
                    ? Protocol776.packetId(ProtocolState.PLAY, Direction.C2S, "move_player_pos")
                    : Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "move_entity_pos"),
                i < 2 ? Frame.NO_PING : i,
                bytes(0x40 + i, 3 + i)
            ));
        }
        return List.copyOf(frames);
    }

    /// A section with an indirect palette, so the fixture exercises the palette path rather than
    /// only the single-value one air uses.
    static Section palettedSection() {
        var writer = new ByteWriter();
        writer.i16(4096).i16(0);
        writer.u8(4).varIntArray(new int[]{0, 9, 42});
        var data = new long[Section.longCount(4, Section.BLOCK_ENTRY_COUNT)];
        for (int i = 0; i < data.length; i++) data[i] = 0x0123_4567_89AB_CDEFL + i;
        writer.fixedLongArray(data);
        writer.u8(0).varInt(3);
        return Section.decode(new ByteReader(writer.toByteArray()));
    }

    static byte[] bytes(int seed, int length) {
        var value = new byte[length];
        for (int i = 0; i < length; i++) value[i] = (byte) (seed + i * 31);
        return value;
    }

    static TraceHeader write(Path path) {
        return TraceWriter.assemble(path, header(), prelude(), TraceFixture::chunks,
            FrameSource.of(frames()));
    }

    static void assertMatches(Trace trace) {
        assertEquals(header()
            .withCounters(new TraceHeader.Counters(10, totalFrameBytes(), 2, 2, 17)), trace.header());
        assertFramesEqual(prelude(), trace.prelude());
        assertFramesEqual(frames(), trace.frames());
        assertChunksEqual(chunks(), trace.chunks());
    }

    static long totalFrameBytes() {
        return frames().stream().mapToLong(frame -> frame.bytes().length).sum();
    }

    /// [Frame] carries `byte[]`, so records compare it by identity; these are the deep comparisons
    /// the round trip actually means.
    static void assertFramesEqual(List<Frame> expected, List<Frame> actual) {
        assertEquals(expected.size(), actual.size(), "frame count");
        for (int i = 0; i < expected.size(); i++) {
            var want = expected.get(i);
            var got = actual.get(i);
            assertEquals(want.tNs(), got.tNs(), "frame " + i + " tNs");
            assertEquals(want.direction(), got.direction(), "frame " + i + " direction");
            assertEquals(want.state(), got.state(), "frame " + i + " state");
            assertEquals(want.packetId(), got.packetId(), "frame " + i + " packetId");
            assertEquals(want.pingId(), got.pingId(), "frame " + i + " pingId");
            assertArrayEquals(want.bytes(), got.bytes(), "frame " + i + " bytes");
        }
    }

    static void assertChunksEqual(List<WorldChunk> expected, List<WorldChunk> actual) {
        assertEquals(expected.size(), actual.size(), "chunk count");
        for (int i = 0; i < expected.size(); i++) {
            var want = expected.get(i);
            var got = actual.get(i);
            assertEquals(want.chunkX(), got.chunkX(), "chunk " + i + " x");
            assertEquals(want.chunkZ(), got.chunkZ(), "chunk " + i + " z");
            assertEquals(want.sections().size(), got.sections().size(), "chunk " + i + " section count");
            for (int s = 0; s < want.sections().size(); s++) {
                switch (want.sections().get(s)) {
                    case WorldChunk.SectionEntry.Inline(Section section) -> {
                        WorldChunk.SectionEntry.Inline inline = assertInstanceOf(
                            WorldChunk.SectionEntry.Inline.class, got.sections().get(s), "chunk " + i + " section " + s);
                        assertArrayEquals(encode(section), encode(inline.section()), "chunk " + i + " section " + s);
                    }
                    case WorldChunk.SectionEntry.ByHash(byte[] hash) -> {
                        WorldChunk.SectionEntry.ByHash byHash = assertInstanceOf(
                            WorldChunk.SectionEntry.ByHash.class, got.sections().get(s), "chunk " + i + " section " + s);
                        assertArrayEquals(hash, byHash.hash(), "chunk " + i + " section " + s);
                    }
                }
            }
        }
    }

    private static byte[] encode(Section section) {
        var writer = new ByteWriter();
        section.encode(writer);
        return writer.toByteArray();
    }
}
