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
        var index = new ArrayList<>(preamble.index());

        var out = NetworkBuffer.resizableBuffer(2048); // TODO: smarter size
        out.advanceWrite(ReplayHeader.HEADER_LENGTH); // patched once the chunks are laid out

        var metadata = NetworkBuffer.makeArray(NetworkBuffer.NBT_COMPOUND, preamble.metadata());
        out.write(NetworkBuffer.RAW_BYTES, metadata);
        out.advanceWrite(header.indexLength()); // patched below, the index keeps its encoded size

        var preambleLength = (int) out.writeIndex();

        var loadedSegmentIndex = -1;
        byte[] loadedSegment = null;

        for (var i = 0; i < index.size(); i++) {
            var chunk = index.get(i);
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

            var byteOffset = out.writeIndex();
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

                out.ensureWritable(recompressedLength);
                NetworkBuffer.copy(
                    NetworkBuffer.wrap(recompressed, 0, recompressedLength),
                    0,
                    out,
                    byteOffset,
                    recompressedLength
                );
            }

            // Offsets are absolute in a compacted replay rather than packed segment offsets.
            index.set(i, chunk.withCompaction(byteOffset, (int) recompressedLength));
            out.advanceWrite(recompressedLength);

            // todo should reuse the same zstd context here and for the initial write as a small optimization
        }

        var length = out.writeIndex();

        out.writeIndex(0);
        header.write(out);

        out.writeIndex(header.indexByteOffset());
        for (var chunk : index)
            out.write(ChunkIndex.NETWORK_TYPE, chunk);
        if (out.writeIndex() != preambleLength)
            throw new IllegalStateException("compacted replay index does not match its declared length");

        out.writeIndex(length);
        return new Result(out.read(NetworkBuffer.RAW_BYTES), preambleLength);
    }
}
