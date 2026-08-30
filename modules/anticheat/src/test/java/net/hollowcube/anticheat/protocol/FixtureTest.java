package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/// Replays real 26.2 captures through the registry and the decoders: every frame has to resolve to
/// a known packet, and every decodable frame has to survive decode then encode byte for byte.
///
/// The fixtures are optional. Without them the test is skipped rather than silently passing, so a
/// checkout that has not fetched them still builds.
class FixtureTest {

    @Test
    void testEveryFrameResolvesAndRoundTrips() {
        var fixtures = fixtures();
        Assumptions.assumeFalse(fixtures.isEmpty(), "no 776 capture fixtures present");

        for (var fixture : fixtures) {
            var capture = FixtureReader.read(fixture);
            var source = fixture.getFileName().toString();
            assertEquals(Protocol776.PROTOCOL_VERSION, capture.header().pv(), source);
            assertTrue(capture.frames().size() > 100, source + " looks empty");

            int decoded = 0;
            for (var frame : capture.frames()) {
                var where = source + " " + frame.state() + " " + frame.direction() + " id " + frame.packetId();
                var entry = Protocol776.lookup(frame.state(), frame.direction(), frame.packetId());
                assertNotSame(Protocol776.UNKNOWN, entry, where);

                var decoder = entry.decoder();
                if (decoder == null) continue;

                var reader = new ByteReader(frame.body());
                var packet = decoder.decode(reader);
                assertEquals(0, reader.remaining(), where + " (" + entry.name() + ") left bytes unread");
                assertArrayEquals(frame.body(), packet.toByteArray(), where + " (" + entry.name() + ") re-encoded differently");
                decoded++;
            }
            assertTrue(decoded > 0, source + " decoded nothing");
        }
    }

    private static List<Path> fixtures() {
        var directory = FixtureTest.class.getResource("/fixtures/" + Protocol776.PROTOCOL_VERSION);
        if (directory == null) return List.of();
        try (Stream<Path> files = Files.list(Path.of(directory.toURI()))) {
            var result = new ArrayList<Path>(files.filter(f -> f.toString().endsWith(".hcpt.zst")).toList());
            result.sort(Path::compareTo);
            return List.copyOf(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
