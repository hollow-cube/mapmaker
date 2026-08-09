package dev.hollowcube.replay.io;

import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.ReplayEventRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class SegmentedFileReplayStorageTest {

    @Test
    void loadsAndAppendsWithoutOverwritingCommittedSegments(@TempDir Path temporaryDirectory) throws IOException {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var registry = ReplayEventRegistry.builder().build();
        var worldId = UUID.randomUUID();
        var worldVersion = ReplayHeader.worldVersion(UUID.randomUUID());

        var initial = ReplayRecorder.create(registry, storage.writer("run", null), worldId, worldVersion, () -> {
        });
        initial.advance();
        initial.advance();
        initial.close().join();

        var first = storage.load("run");
        assertNotNull(first);
        assertFalse(first.finished());
        assertEquals(2, first.preamble().header().tickCount());
        assertEquals(1, first.preamble().header().chunkCount());
        assertEquals(1, first.preamble().nextSegmentIndex());

        var firstSegmentPath = temporaryDirectory.resolve("run/segment-000.dat");
        var firstSegment = Files.readAllBytes(firstSegmentPath);

        var resumed = ReplayRecorder.resume(
            registry,
            storage.writer("run", first),
            first.preamble(),
            worldId,
            worldVersion,
            () -> {
            }
        );
        resumed.advance();
        resumed.close().join();

        var appended = storage.load("run");
        assertNotNull(appended);
        assertEquals(3, appended.preamble().header().tickCount());
        assertEquals(2, appended.preamble().header().chunkCount());
        assertEquals(2, appended.preamble().nextSegmentIndex());
        assertArrayEquals(firstSegment, Files.readAllBytes(firstSegmentPath));
        assertTrue(Files.size(temporaryDirectory.resolve("run/segment-001.dat")) > 0);
    }

    @Test
    void finishedRecordingsAreLoadedAsFinished(@TempDir Path temporaryDirectory) {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var recorder = ReplayRecorder.create(
            ReplayEventRegistry.builder().build(),
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        recorder.advance();
        recorder.finish().join();

        var replay = storage.load("run");
        assertNotNull(replay);
        assertTrue(replay.finished());
    }

    @Test
    void rejectsPreambleThatReferencesATruncatedSegment(@TempDir Path temporaryDirectory) throws IOException {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var recorder = ReplayRecorder.create(
            ReplayEventRegistry.builder().build(),
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );
        recorder.advance();
        recorder.close().join();

        Files.write(
            temporaryDirectory.resolve("run/segment-000.dat"),
            new byte[0],
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
        );

        assertThrows(IllegalArgumentException.class, () -> storage.load("run"));
    }

    @Test
    void rejectsReplayIdsThatEscapeTheStorageRoot(@TempDir Path temporaryDirectory) {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);

        assertThrows(IllegalArgumentException.class, () -> storage.load("../run"));
        assertThrows(IllegalArgumentException.class, () -> storage.writer("/run", null));
    }
}
