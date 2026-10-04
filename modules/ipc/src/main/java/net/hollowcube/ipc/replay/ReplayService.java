package net.hollowcube.ipc.replay;

import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.util.Ipc;
import org.jetbrains.annotations.Nullable;

/// Replay storage: the recorder's append log while a run is being recorded, and the one object it
/// becomes afterwards.
///
/// Storage never reads the bytes. Everything it needs it is told — the preamble length, the body
/// digest, whether this is the last commit and why — which is what keeps the replay format out of
/// the api entirely.
@Ipc
public interface ReplayService {

    /// Null for a replay nothing has committed yet, which the first commit creates.
    @Nullable
    ReplayInfo getReplay(String id);

    /// `expectedRevision` is the one [#getReplay] answered with, since the recorder resumes from
    /// the state and the preamble together; a preamble that has moved on is a 412. Null reads
    /// whatever is there.
    Blob getPreamble(String id, @Nullable Long expectedRevision);

    /// One committed segment, whole. Segments are immutable, so there is nothing to be conditional
    /// about.
    Blob getSegment(String id, int segmentIndex);

    /// The compacted recording, or one absolute byte range of it — both bounds or neither, which is
    /// how playback pulls a single frame. 409 while the replay is still segmented.
    Blob getCompacted(String id, @Nullable Long start, @Nullable Long endInclusive);

    /// One atomic commit: at most one new segment, the complete replacement preamble, and
    /// optionally the transition to finished. The body is `preamble || segment`, split at
    /// [ReplayCommit#preambleLength].
    ReplayInfo commit(ReplayCommit meta, Blob body);

    /// Drops the segments a compacted replay was built from, and their objects.
    ///
    /// A bad compaction is only repairable while its sources are still there, so this is the
    /// sweeper's after a grace period and nothing else's. Refused unless the replay is compacted.
    ///
    /// @return how many segment rows were dropped
    int dropSegments(String id);

    /// Drops a recording's segments below `segmentIndex`, which its preamble at `expectedRevision`
    /// no longer references: a recording rewritten in place keeps resuming from the one new segment
    /// the rewrite committed, and everything before it is dead. Storage cannot read the preamble,
    /// so the caller says where the cut is and the revision is what makes that still true.
    ///
    /// @return how many segment rows were dropped
    int dropSegmentsBefore(String id, long expectedRevision, int segmentIndex);

    /// Finds the objects under one shard of the bucket that no replay points at — staged writes
    /// whose commit was refused or lost, and compacted objects replaced before publication removed
    /// them — and deletes them when `deleting`. Objects younger than a day are left alone, since an
    /// upload whose row has not landed yet looks the same.
    ///
    /// @param shard leading hex digits of the hashed replay id, the unit the sweep is split into
    /// @return how many orphans the shard held
    int sweepOrphanedObjects(String shard, boolean deleting);

    /// Publishes a compacted recording over a finished segmented one.
    ///
    /// A call rather than a direct write by the worker so that the api-server stays the one process
    /// holding object-store credentials, and the pointer swap happens under the lock that guards
    /// every other write to the row.
    ReplayInfo publishCompacted(ReplayCompaction meta, Blob body);
}
