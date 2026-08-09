package net.hollowcube.mapmaker.runtime.parkour.replay;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.ReplayPlayer;
import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.AbsoluteMoveEvent;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.io.CompactedReplayReader;
import dev.hollowcube.replay.io.SegmentedFileReplaySource;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.CheckpointReachedEvent;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.CheckpointResetEvent;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.ClearGhostBlocksEvent;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.RunEndEvent;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.RunStartEvent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Records a run's worth of parkour events through the parkour registry and plays them back, so
/// that the host events keep the IDs and payloads every recorded replay already assumes.
final class ParkourReplayEventRoundTripTest {

    @Test
    void parkourEventsSurviveCompactionAndPlayback(@TempDir Path temporaryDirectory) {
        var storage = new SegmentedFileReplayStorage(temporaryDirectory);
        var recorder = ReplayRecorder.create(
            ReplayManager.REGISTRY,
            storage.writer("run", null),
            UUID.randomUUID(),
            ReplayHeader.worldVersion(UUID.randomUUID()),
            () -> {
            }
        );

        // Interleaved with a generic event, because the point of appending is that both halves of
        // the registry stay readable in the same replay.
        var expected = List.<ReplayEvent>of(
            new RunStartEvent(),
            new AbsoluteMoveEvent(0, new Pos(1, 64, 1), Vec.ZERO),
            new CheckpointReachedEvent("checkpoint-one", 1_500),
            new CheckpointResetEvent(CheckpointResetEvent.Reason.RESET_HEIGHT, 4_250),
            new CheckpointReachedEvent("checkpoint-two", 9_000),
            new CheckpointResetEvent(CheckpointResetEvent.Reason.LIVES, 12_000),
            new RunEndEvent(RunEndEvent.Reason.FINISH, 30_125),
            new ClearGhostBlocksEvent()
        );
        for (var event : expected) {
            recorder.submit(event);
            recorder.advance();
        }
        recorder.finish().join();

        var recording = storage.load("run");
        assertNotNull(recording);

        var compacted = ReplayCompactor.compact(
            recording.requirePreamble(),
            new SegmentedFileReplaySource(temporaryDirectory.resolve("run"))
        );

        var played = new ArrayList<ReplayEvent>();
        try (var player = new ReplayPlayer(
            new CompactedReplayReader(compacted.data()), ReplayManager.REGISTRY, played::add)) {
            while (player.advance() == ReplayPlayer.Advance.ADVANCED) ;
        }

        assertEquals(expected, played);
    }
}
