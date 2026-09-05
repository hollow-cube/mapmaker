package net.hollowcube.ipc.replay;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

/// What storage knows about a replay, minus its bytes.
///
/// @param revision         which stored revision this is, handed back on the next conditional
///                         write. It moves on a compaction as well as on a commit, so it
///                         identifies the revision and not the recording
/// @param nextSegmentIndex null once compacted, since there is nothing left to append
/// @param preambleLength   so a caller can size a buffer before fetching it
/// @param outcome          null on every replay the Go server wrote and on anything a client older
///                         than the field finished
/// @param updatedAt        epoch milliseconds
@RuntimeGson
public record ReplayInfo(
    String id,
    long revision,
    ReplayState state,
    ReplayRepresentation representation,
    @Nullable Integer nextSegmentIndex,
    int preambleLength,
    @Nullable ReplayOutcome outcome,
    long updatedAt
) {
}
