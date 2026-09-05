package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEventRegistry;
import dev.hollowcube.replay.io.RunOutcome;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/// Records a replay, chunk by chunk.
///
/// The recorder alone decides where chunk boundaries fall, since only it knows how large the
/// encoded ticks have grown. Seeking lands on a chunk boundary, so each chunk has to open with
/// enough state to stand on its own; the recorder therefore calls the host's `snapshot` runnable at
/// the start of the first tick of every chunk, before any of the host's own events for that tick.
/// The host is expected to submit whatever fully describes the world during that tick (spawns,
/// absolute positions, inventories, ...) rather than deltas against what it last recorded.
public sealed interface ReplayRecorder permits ReplayRecorderImpl {

    static ReplayRecorder create(
        ReplayEventRegistry registry,
        SegmentedReplayWriter writer,
        UUID worldId,
        byte[] worldVersion,
        Runnable snapshot
    ) {
        return new ReplayRecorderImpl(registry, writer, worldId, worldVersion, snapshot);
    }

    static ReplayRecorder resume(
        ReplayEventRegistry registry,
        SegmentedReplayWriter writer,
        ReplayPreamble preamble,
        UUID worldId,
        byte[] worldVersion,
        Runnable snapshot
    ) {
        preamble.requireCompatible(worldId, worldVersion);
        return new ReplayRecorderImpl(registry, writer, preamble, snapshot);
    }

    /// Advance the replay to the next tick.
    /// This should be called once per tick, after all events have been submitted.
    void advance();

    void submit(ReplayEvent event);

    /// The number of complete ticks recorded so far, including previously committed ones.
    int tick();

    /// A snapshot of how much this recording has accumulated, for debug display.
    Stats stats();

    /// @param tick        as [ReplayRecorder#tick()]
    /// @param bytes       compressed bytes of every chunk flushed so far, whether or not it has
    ///                    reached storage yet. The open chunk is still uncompressed, so it is not
    ///                    counted; neither is the preamble.
    /// @param chunks      the number of complete chunks, including previously committed ones
    /// @param events      events submitted to this recorder. A resumed recording cannot know what
    ///                    earlier sessions submitted, so it counts from zero again.
    /// @param chunkEvents events submitted since the last chunk boundary
    record Stats(int tick, long bytes, int chunks, int events, int chunkEvents) {
    }

    /// True once any part of this recording has been sent to storage.
    ///
    /// A recording that has not is still entirely local, so a host that decides it is not worth
    /// keeping can [#discard()] it and leave nothing behind.
    boolean committed();

    /// The failure that killed this recording, or null while it is still healthy.
    ///
    /// A write failure is terminal. The recorder stops accepting ticks and events rather than
    /// accumulating an index it can never persist, so hosts should poll this and drop the
    /// recording rather than waiting until [#close()] to discover it.
    @Nullable Throwable failure();

    /// Commits all completed ticks without closing the writer.
    ///
    /// The currently open tick is discarded and reopened at the same tick index, so recording can
    /// resume later. Hosts should call [#advance()] at their end-of-tick boundary first.
    CompletableFuture<Void> flush();

    /// Commits all completed ticks and closes the underlying writer, leaving the recording
    /// resumable by a later session.
    ///
    /// The currently open tick is not included. Hosts should call [#advance()] at their
    /// end-of-tick boundary before closing a recording.
    CompletableFuture<Void> close();

    /// Commits all completed ticks as a final commit and closes the underlying writer.
    ///
    /// The recording is permanently complete once this succeeds; it can never be appended to
    /// again, and storage may begin compacting it. If there is nothing left to commit this still
    /// emits a commit carrying only the preamble, so that the finished transition is durable.
    ///
    /// The first of [#close()] or this call wins; the other returns the same future.
    CompletableFuture<Void> finish(RunOutcome outcome);

    /// Closes the underlying writer without committing anything, abandoning the whole recording.
    ///
    /// Only valid while nothing has been [#committed()], since bytes that have reached storage
    /// cannot be recalled. As with the others, the first termination wins.
    CompletableFuture<Void> discard();

}
