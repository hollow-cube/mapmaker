package net.hollowcube.apiserver.job;

import org.jetbrains.annotations.Nullable;

/// Asks for a finished segmented replay to be merged into one compacted object, as the data of a
/// [JobSpec#COMPACT_REPLAY] row. By hand:
///
/// ```sql
/// insert into jobs (job, instance, data)
/// values ('compact-replay', '<replay id>', '{"replayId": "<replay id>", "reason": "manual"}');
/// ```
///
/// No source revision in here: a later commit would invalidate it, and `enqueueJob` coalesces the
/// data of a row asked for twice, so the runner reads the current state instead.
///
/// @param reason why, for the log; nothing reads it
public record CompactReplay(String replayId, @Nullable String reason) {
}
