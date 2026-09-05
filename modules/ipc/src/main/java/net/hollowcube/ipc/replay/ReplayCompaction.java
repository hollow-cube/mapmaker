package net.hollowcube.ipc.replay;

import net.hollowcube.common.util.RuntimeGson;

/// Everything about one compacted publication except its bytes, which are the body — the whole
/// compacted replay, preamble included.
///
/// @param expectedRevision required: a compaction is only valid against the revision it was built
///                       from, and a commit that landed in between means there is more to compact
/// @param idempotencyKey derived from the replay and the source etag rather than random, so that
///                       a retry after a lost response returns the first publication instead of
///                       uploading a second object and orphaning the first
/// @param preambleLength the prefix of the body storage keeps as the replay's preamble
/// @param contentDigest  base64 of the SHA-256 of the whole body
@RuntimeGson
public record ReplayCompaction(
    String id,
    long expectedRevision,
    String idempotencyKey,
    int preambleLength,
    String contentDigest
) {
}
