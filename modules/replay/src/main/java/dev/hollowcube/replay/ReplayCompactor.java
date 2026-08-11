package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import net.minestom.server.network.NetworkBuffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.function.IntFunction;

/// Turns a finished segmented recording into a single self-contained replay.
///
/// Compaction serves a few purposes:
/// 1. better compression
/// 2. better read prefetching
/// 3. unifying all segments to a single game/event/dictionary version
///
/// It is deliberately ignorant of where segments come from, so the same code compacts a local
/// recording and one downloaded from replay storage.
public final class ReplayCompactor {

    // TODO: in the future we may need to deal with not loading the entire thing into memory, for now its ignored.

    private ReplayCompactor() {
    }

    /// A compacted replay, and the length of the preamble prefix that storage retains separately.
    public record Result(byte[] data, int preambleLength) {
    }

    /// Compacts a recording, reading each referenced segment exactly once through `segments`.
    public static Result compact(ReplayPreamble preamble, IntFunction<byte[]> segments) {
        var header = preamble.header();
        var metadata = NetworkBuffer.makeArray(NetworkBuffer.NBT_COMPOUND, preamble.metadata());

        // Chunks are laid out first, into a buffer of their own, because recompression is what
        // decides their new lengths, the index cannot be encoded until it knows them, and the
        // chunks cannot be placed until the index in front of them has its final size.
        var chunks = NetworkBuffer.resizableBuffer(2048); // TODO: smarter size
        var index = new ArrayList<ChunkIndex>(preamble.index().size());

        var loadedSegmentIndex = -1;
        byte[] loadedSegment = null;

        for (var chunk : preamble.index()) {
            var segmentIndex = ReplayPreamble.segmentIndex(chunk);
            var segmentOffset = ReplayPreamble.segmentOffset(chunk);

            // The index is ordered by segment, so holding one at a time is enough to read each
            // exactly once rather than re-reading it per chunk.
            if (segmentIndex != loadedSegmentIndex) {
                loadedSegment = segments.apply(segmentIndex);
                loadedSegmentIndex = segmentIndex;
            }
            if (segmentOffset + chunk.compressedLength() > loadedSegment.length)
                throw new IllegalStateException("replay chunk lies outside segment " + segmentIndex);

            var byteOffset = chunks.writeIndex();
            long recompressedLength;
            try (var arena = Arena.ofConfined()) {
                var compressed = arena.allocate(chunk.compressedLength());
                MemorySegment.copy(
                    MemorySegment.ofArray(loadedSegment),
                    segmentOffset,
                    compressed,
                    0,
                    chunk.compressedLength()
                );

                var uncompressed = arena.allocate(chunk.uncompressedLength());
                var uncompressedLength = Zstd.decompressUnsafe(
                    uncompressed.address(),
                    uncompressed.byteSize(),
                    compressed.address(),
                    compressed.byteSize()
                );
                if (Zstd.isError(uncompressedLength)) {
                    throw new IllegalStateException("Replay decompression failed: " + Zstd.getErrorName(uncompressedLength));
                }
                if (uncompressedLength != chunk.uncompressedLength()) {
                    throw new IllegalStateException(
                        "Replay decompression length mismatch: expected "
                            + chunk.uncompressedLength() + ", got " + uncompressedLength
                    );
                }

                var recompressed = arena.allocate(Zstd.compressBound(chunk.uncompressedLength()));
                recompressedLength = Zstd.compressUnsafe(
                    recompressed.address(),
                    recompressed.byteSize(),
                    uncompressed.address(),
                    uncompressedLength,
                    ReplayHeader.COMPACT_COMPRESSION_LEVEL
                );
                if (Zstd.isError(recompressedLength)) {
                    throw new IllegalStateException("Replay compression failed: " + Zstd.getErrorName(recompressedLength));
                }

                chunks.ensureWritable(recompressedLength);
                NetworkBuffer.copy(
                    NetworkBuffer.wrap(recompressed, 0, recompressedLength),
                    0,
                    chunks,
                    byteOffset,
                    recompressedLength
                );
            }

            // Offsets are still relative to the first chunk here; they become absolute below, once
            // the preamble in front of them has a length.
            index.add(chunk.withCompaction(byteOffset, (int) recompressedLength));
            chunks.advanceWrite(recompressedLength);

            // todo should reuse the same zstd context here and for the initial write as a small optimization
        }

        // A chunk index encodes its offset as a fixed-width long and everything else is final by
        // now, so measuring the index on relative offsets gives the same length the absolute ones
        // will take. Recompression can narrow a chunk's length past a varint boundary, so this
        // cannot assume the index is as long as the segmented recording's was.
        var indexLength = NetworkBuffer.makeArray(buffer -> {
            for (var chunk : index) buffer.write(ChunkIndex.NETWORK_TYPE, chunk);
        }).length;
        var preambleLength = ReplayHeader.HEADER_LENGTH + metadata.length + indexLength;
        header.update(metadata.length, indexLength, header.tickCount(), index.size());

        var chunkLength = chunks.readableBytes();
        var out = NetworkBuffer.resizableBuffer((int) Math.min(preambleLength + chunkLength, Integer.MAX_VALUE));
        header.write(out);
        out.write(NetworkBuffer.RAW_BYTES, metadata);
        for (var chunk : index)
            out.write(ChunkIndex.NETWORK_TYPE, chunk.withCompaction(
                preambleLength + chunk.byteOffset(), chunk.compressedLength()));
        if (out.writeIndex() != preambleLength)
            throw new IllegalStateException("compacted replay preamble does not match its declared length");

        out.ensureWritable(chunkLength);
        NetworkBuffer.copy(chunks, 0, out, out.writeIndex(), chunkLength);
        out.advanceWrite(chunkLength);

        return new Result(out.read(NetworkBuffer.RAW_BYTES), preambleLength);
    }
}
