package net.hollowcube.ipc.replay;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

/// Everything about one recording commit except its bytes, which are the body.
///
/// @param expectedRevision what [ReplayInfo#revision] last said; null creates the replay, and is
///                       the only way to. There are no unconditional writes
/// @param idempotencyKey stable across the retries of one commit, which is what lets a recorder
///                       that lost the response send the same bytes again and be told what it was
///                       told the first time rather than installing a second segment
/// @param segmentIndex   null only for a metadata-only final commit, whose body is the preamble
///                       alone
/// @param outcome        why, when `finished`; ignored otherwise
/// @param contentDigest  base64 of the SHA-256 of the whole body, `preamble || segment`
@RuntimeGson
public record ReplayCommit(
    String id,
    @Nullable Long expectedRevision,
    String idempotencyKey,
    int preambleLength,
    @Nullable Integer segmentIndex,
    boolean finished,
    @Nullable ReplayOutcome outcome,
    String contentDigest
) {
}
