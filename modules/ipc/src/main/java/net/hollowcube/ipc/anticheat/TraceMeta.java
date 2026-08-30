package net.hollowcube.ipc.anticheat;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

/// What the store files a capture trace under: the trace's own header, cut to the fields a row
/// is found by, so that answering "every trace of this run" never means opening a blob.
///
/// Only the two a row is useless without are required. The rest are missing on purpose sometimes —
/// a ring flush has no capture, an older proxy build had no name — and a row without them beats
/// refusing a trace there is no second copy of.
///
/// @param id            the proxy's name for the trace, and the blob's on the volume, so it has
///                      to survive being a path component
/// @param captureId     what the backend asked for, and null for a flush nobody asked for
/// @param proxyVersion  the commit the proxy plugin that wrote the trace was built from
/// @param clientPvn     the protocol version the frames are in, or zero when nobody recorded one
/// @param reason        why the trace was captured, as the table spells it: `run`, `sample`,
///                      `flag` or `manual`
/// @param startedAt     when the capture started, in epoch milliseconds; also what dates the blob's
///                      path, so a ship retried past midnight names the same file
/// @param endedAt       when it was closed, null for one cut short before it was
/// @param formatVersion what the proxy says the container is. The store reads the real one off the
///                      bytes and keeps that; this is only ever the claim.
@RuntimeGson
public record TraceMeta(
    String id,
    @Nullable String captureId,
    String playerId,
    @Nullable String proxyVersion,
    @Nullable String proxy,
    int clientPvn,
    @Nullable String reason,
    long startedAt,
    @Nullable Long endedAt,
    int formatVersion
) {
}
