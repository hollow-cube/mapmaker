package net.hollowcube.apiworker.jobs;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.mapmaker.runtime.parkour.replay.ReplayManager;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A replay's stored bytes, whichever representation they are in, fetched whole.
///
/// @param compacted null while the replay is segmented, and `segments` empty once it is not
record StoredReplay(
    ReplayInfo info,
    byte[] preamble,
    List<byte[]> segments,
    byte @Nullable [] compacted
) {

    /// Whether any chunk was written at older versions than this build writes. False for a
    /// preamble this build cannot read at all, which nothing can rewrite.
    static boolean needsRewrite(byte[] preamble) {
        if (!ReplayHeader.readable(ReplayHeader.versionOf(preamble))) return false;
        return ReplayPreamble.index(preamble)
            .stream()
            .anyMatch(
                chunk -> chunk.formatVersion() != ReplayHeader.VERSION_LATEST
                    || chunk.dataVersion() != MinecraftServer.DATA_VERSION
            );
    }

    static StoredReplay read(
        ReplayService replays,
        ReplayInfo info,
        byte[] preamble
    ) throws IOException {
        var id = info.id();
        if (info.representation() == ReplayRepresentation.COMPACTED) {
            try (var blob = replays.getCompacted(id, null, null)) {
                return new StoredReplay(info, preamble, List.of(), blob.readAllBytes());
            }
        }
        if (info.representation() != ReplayRepresentation.SEGMENTED)
            throw new IllegalStateException(
                "replay " + id + " has an unknown representation " + info.representation()
            );

        var count = ReplayPreamble.read(preamble).nextSegmentIndex();
        var segments = new ArrayList<byte[]>(count);
        for (var index = 0; index < count; index++) {
            try (var blob = replays.getSegment(id, index)) {
                segments.add(blob.readAllBytes());
            }
        }
        return new StoredReplay(info, preamble, segments, null);
    }

    long storedBytes() {
        if (compacted != null) return compacted.length;
        var total = (long) preamble.length;
        for (var segment : segments) total += segment.length;
        return total;
    }

    long rawBytes() {
        return rawBytes(ReplayPreamble.index(preamble));
    }

    static long rawBytes(List<ChunkIndex> index) {
        var total = 0L;
        for (var chunk : index) total += chunk.uncompressedLength();
        return total;
    }

    ReplayCompactor.Result transcode() {
        if (compacted != null)
            return ReplayCompactor.transcode(compacted, null, ReplayManager.REGISTRY);
        return ReplayCompactor.transcode(
            ReplayPreamble.read(preamble),
            segments::get,
            null,
            ReplayManager.REGISTRY
        );
    }

    /// For the sample, to separate what compaction saves from what transcoding costs.
    byte[] untranscoded() {
        if (compacted != null) return compacted;
        return ReplayCompactor
            .compact(ReplayPreamble.read(preamble), segments::get, null, ReplayManager.REGISTRY)
            .data();
    }
}
