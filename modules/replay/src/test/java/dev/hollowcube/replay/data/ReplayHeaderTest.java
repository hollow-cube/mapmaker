package dev.hollowcube.replay.data;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class ReplayHeaderTest {

    @Test
    void uuidWorldVersionSurvivesRoundTrip() {
        assertRoundTrip(ReplayHeader.worldVersion(UUID.randomUUID()));
    }

    @Test
    void maximumLengthWorldVersionSurvivesRoundTrip() {
        // A tail of zeroes is what the length prefix exists to distinguish from a shorter version.
        var worldVersion = new byte[ReplayHeader.WORLD_VERSION_MAX_LENGTH];
        for (var i = 0; i < 8; i++) worldVersion[i] = (byte) (i + 1);
        assertRoundTrip(worldVersion);
    }

    @Test
    void emptyWorldVersionSurvivesRoundTrip() {
        assertRoundTrip(new byte[0]);
    }

    @Test
    void tooLongWorldVersionIsRejected() {
        var worldVersion = new byte[ReplayHeader.WORLD_VERSION_MAX_LENGTH + 1];
        assertThrows(IllegalArgumentException.class, () -> new ReplayHeader(UUID.randomUUID(), worldVersion));
    }

    @Test
    void corruptWorldVersionLengthIsRejected() {
        var written = write(new ReplayHeader(UUID.randomUUID(), ReplayHeader.worldVersion(UUID.randomUUID())));
        // Just past magic, version, flags and the world id.
        written[24] = (byte) 0xFF;

        var buffer = NetworkBuffer.wrap(written, 0, written.length);
        assertThrows(IllegalArgumentException.class, () -> new ReplayHeader(buffer));
    }

    @Test
    void anyOtherFormatVersionIsRejected() {
        var written = write(new ReplayHeader(UUID.randomUUID(), ReplayHeader.worldVersion(UUID.randomUUID())));
        // Every field after the version moved at least once, so an older layout read as this one is
        // shifted garbage rather than a slightly stale replay.
        written[4] = 0;
        written[5] = 1;

        var buffer = NetworkBuffer.wrap(written, 0, written.length);
        var error = assertThrows(IllegalArgumentException.class, () -> new ReplayHeader(buffer));
        assertTrue(error.getMessage().contains("unsupported replay version: 1"), error.getMessage());
    }

    @Test
    void aLegacyVersionIsReadAtTheOnlyDataVersionItWasWrittenWith() {
        var written = legacy(ReplayHeader.LEGACY_IDS_DATA_VERSION);
        var read = new ReplayHeader(NetworkBuffer.wrap(written, 0, written.length));
        assertEquals(ReplayHeader.VERSION_LEGACY_IDS, read.version());

        var error = assertThrows(IllegalArgumentException.class, () -> new ChunkIndex(0, 1, (byte) 0, 0, 1, 1,
            ReplayHeader.VERSION_LEGACY_IDS, 4786));
        assertTrue(error.getMessage().contains("has no ID mapping"), error.getMessage());
    }

    @Test
    void aHeaderReadAtTheLegacyVersionIsWrittenAtTheLatest() {
        var written = legacy(ReplayHeader.LEGACY_IDS_DATA_VERSION);
        var read = new ReplayHeader(NetworkBuffer.wrap(written, 0, written.length));

        assertEquals(ReplayHeader.VERSION_LATEST, ReplayHeader.versionOf(write(read)));
    }

    /// A format 4 header, whose layout is the latest one with a different version.
    private static byte[] legacy(int dataVersion) {
        var written = write(new ReplayHeader(UUID.randomUUID(), new byte[0]));
        written[5] = ReplayHeader.VERSION_LEGACY_IDS;
        // The data version is the last field, past an empty world version and everything before it.
        var dataVersionOffset = 4 + 2 + 2 + 16 + 1 + 8 + 2 + 4 * 4;
        NetworkBuffer.wrap(written, dataVersionOffset, dataVersionOffset).write(NetworkBuffer.INT, dataVersion);
        return written;
    }

    private static void assertRoundTrip(byte[] worldVersion) {
        var worldId = UUID.randomUUID();
        var header = new ReplayHeader(worldId, worldVersion);
        header.update(1, 2, 3, 4);

        var written = write(header);
        assertEquals(ReplayHeader.HEADER_LENGTH, written.length);

        var buffer = NetworkBuffer.wrap(written, 0, written.length);
        var read = new ReplayHeader(buffer);
        assertEquals(ReplayHeader.HEADER_LENGTH, buffer.readIndex());

        assertEquals(worldId, read.worldId());
        assertTrue(Arrays.equals(worldVersion, read.worldVersion()));
        assertEquals(header.version(), read.version());
        assertEquals(header.timestamp(), read.timestamp());
        assertEquals(header.dictionary(), read.dictionary());
        assertEquals(header.dataVersion(), read.dataVersion());
        assertEquals(1, read.metadataLength());
        assertEquals(2, read.indexLength());
        assertEquals(3, read.tickCount());
        assertEquals(4, read.chunkCount());
    }

    private static byte[] write(ReplayHeader header) {
        var written = new byte[ReplayHeader.HEADER_LENGTH];
        header.write(NetworkBuffer.wrap(written, 0, 0));
        return written;
    }
}
