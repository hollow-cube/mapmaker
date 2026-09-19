package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.event.ReplayEventRegistry;
import dev.hollowcube.replay.event.ReplayEvents;
import dev.hollowcube.replay.io.CompactedReplayReader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/// Turns a finished segmented recording into a single self-contained replay.
///
/// Compaction serves a few purposes:
/// 1. better compression
/// 2. better read prefetching
/// 3. unifying all segments to a single game/event/dictionary version
///
/// Frames never merge chunks written at different versions, since a frame has one set to be read
/// with.
///
/// It is deliberately ignorant of where segments come from, so the same code compacts a local
/// recording and one downloaded from replay storage.
///
/// Chunks are merged into frames of up to [#FRAME_BYTE_LIMIT] raw bytes before being recompressed:
/// recompressing each chunk alone bought 6% of the production corpus, merging first buys 41%.
/// Written as a [ReplayVisitor] so anything else that wants every frame rides the same pass.
public final class ReplayCompactor {

    /// The most playback ever decompresses at once, and the knee of the measured curve: 64 KiB
    /// gives −56%, 256 KiB −60%, 1 MiB −61%, the whole file −62%. Frames split out of an oversized
    /// chunk may lack a snapshot; seeking through those rebuilds from the preceding one.
    public static final int FRAME_BYTE_LIMIT = 256 * 1024;

    private ReplayCompactor() {
    }

    /// A compacted replay, and the length of the preamble prefix that storage retains separately.
    public record Result(byte[] data, int preambleLength) {
    }

    /// Compacts a recording, reading each referenced segment exactly once through `segments`.
    public static Result compact(ReplayPreamble preamble, IntFunction<byte[]> segments) {
        return compact(preamble, segments, null);
    }

    /// With `observer` shown every frame as it is decoded, which is what [ReplayVisitor#walk] would
    /// show it over the result — attaching one here only saves the second decompression.
    public static Result compact(ReplayPreamble preamble, IntFunction<byte[]> segments,
                                 @Nullable ReplayVisitor observer) {
        return compact(preamble, segments, observer, ReplayEvents.builder().build());
    }

    /// The host registry is needed to find tick boundaries when a source chunk exceeds the limit.
    public static Result compact(ReplayPreamble preamble, IntFunction<byte[]> segments,
                                 @Nullable ReplayVisitor observer, ReplayEventRegistry registry) {
        return compact(preamble, segments, observer, FRAME_BYTE_LIMIT, registry);
    }

    /// As above, at a frame size other than the one production uses. Only a test wants this: the
    /// frame size is baked into every compacted replay and the corpus was measured at one value.
    @TestOnly
    public static Result compact(ReplayPreamble preamble, IntFunction<byte[]> segments,
                                 @Nullable ReplayVisitor observer, int frameByteLimit) {
        return compact(preamble, segments, observer, frameByteLimit, ReplayEvents.builder().build());
    }

    @TestOnly
    public static Result compact(ReplayPreamble preamble, IntFunction<byte[]> segments,
                                 @Nullable ReplayVisitor observer, int frameByteLimit, ReplayEventRegistry registry) {
        return build(preamble.header(), preamble.metadata(), visitor -> decode(preamble, segments, visitor),
            observer, frameByteLimit, registry, false);
    }

    /// Compacts a recording, rewriting every event at the current format and data version so that
    /// nothing reading the result needs the versions it was recorded at. Unlike [#compact] this
    /// resolves game data, so the host's registries have to be loaded.
    public static Result transcode(ReplayPreamble preamble, IntFunction<byte[]> segments,
                                   @Nullable ReplayVisitor observer, ReplayEventRegistry registry) {
        return build(preamble.header(), preamble.metadata(), visitor -> decode(preamble, segments, visitor),
            observer, FRAME_BYTE_LIMIT, registry, true);
    }

    /// As above, over a replay that has already been compacted. Its frames are merged again.
    public static Result transcode(byte[] compacted, @Nullable ReplayVisitor observer, ReplayEventRegistry registry) {
        try (var reader = new CompactedReplayReader(compacted)) {
            return build(reader.header(), reader.metadata(), visitor -> ReplayVisitor.walk(reader, visitor),
                observer, FRAME_BYTE_LIMIT, registry, true);
        }
    }

    public record Rewrite(byte[] preamble, byte[] segment) {
    }

    /// Transcodes a recording into one new segment `segmentIndex`, keeping it resumable: chunks are
    /// not merged, and the preamble handed back indexes only that segment.
    public static Rewrite transcodeRecording(ReplayPreamble preamble, IntFunction<byte[]> segments,
                                             int segmentIndex, ReplayEventRegistry registry) {
        var recorded = new RecordedChunks(segmentIndex);
        decode(preamble, segments, new Transcoded(recorded, registry));

        var metadata = NetworkBuffer.makeArray(NetworkBuffer.NBT_COMPOUND, preamble.metadata());
        var index = NetworkBuffer.makeArray(buffer -> {
            for (var chunk : recorded.index) chunk.write(buffer);
        });
        var header = preamble.header();
        header.update(metadata.length, index.length, header.tickCount(), recorded.index.size());
        return new Rewrite(NetworkBuffer.makeArray(buffer -> {
            header.write(buffer);
            buffer.write(NetworkBuffer.RAW_BYTES, metadata);
            buffer.write(NetworkBuffer.RAW_BYTES, index);
        }), recorded.segment.read(NetworkBuffer.RAW_BYTES));
    }

    private static Result build(ReplayHeader header, CompoundBinaryTag sourceMetadata, Consumer<ReplayVisitor> source,
                                @Nullable ReplayVisitor observer, int frameByteLimit, ReplayEventRegistry registry,
                                boolean transcode) {
        if (frameByteLimit <= 0) throw new IllegalArgumentException("frame limit must be positive");
        var metadata = NetworkBuffer.makeArray(NetworkBuffer.NBT_COMPOUND, sourceMetadata);

        var frames = new MergedFrames(frameByteLimit, registry);
        ReplayVisitor visitor = observer == null ? frames : ReplayVisitor.of(List.of(frames, observer));
        if (transcode) visitor = new Transcoded(visitor, registry);
        source.accept(visitor);

        var index = frames.index;
        var chunks = frames.chunks;

        // A chunk index encodes its offset as a fixed-width long and everything else is final by
        // now, so measuring the index on relative offsets gives the same length the absolute ones
        // will take. Recompression can narrow a frame's length past a varint boundary, so this
        // cannot assume the index is as long as the segmented recording's was.
        var indexLength = NetworkBuffer.makeArray(buffer -> {
            for (var chunk : index) chunk.write(buffer);
        }).length;
        var preambleLength = ReplayHeader.HEADER_LENGTH + metadata.length + indexLength;
        header.update(metadata.length, indexLength, header.tickCount(), index.size());

        var chunkLength = chunks.readableBytes();
        var out = NetworkBuffer.resizableBuffer((int) Math.min(preambleLength + chunkLength, Integer.MAX_VALUE));
        header.write(out);
        out.write(NetworkBuffer.RAW_BYTES, metadata);
        for (var chunk : index)
            chunk.withCompaction(preambleLength + chunk.byteOffset(), chunk.compressedLength()).write(out);
        if (out.writeIndex() != preambleLength)
            throw new IllegalStateException("compacted replay preamble does not match its declared length");

        out.ensureWritable(chunkLength);
        NetworkBuffer.copy(chunks, 0, out, out.writeIndex(), chunkLength);
        out.advanceWrite(chunkLength);

        return new Result(out.read(NetworkBuffer.RAW_BYTES), preambleLength);
    }

    /// Every chunk the preamble indexes, decompressed in order.
    private static void decode(ReplayPreamble preamble, IntFunction<byte[]> segments, ReplayVisitor visitor) {
        visitor.open(preamble.header(), preamble.metadata(), preamble.index().size());

        var loadedSegmentIndex = -1;
        byte[] loadedSegment = null;

        for (var chunk : preamble.index()) {
            var segmentIndex = ReplayPreamble.segmentIndex(chunk);
            var segmentOffset = ReplayPreamble.segmentOffset(chunk);

            // Ordered by segment, so holding one at a time reads each exactly once.
            if (segmentIndex != loadedSegmentIndex) {
                loadedSegment = segments.apply(segmentIndex);
                loadedSegmentIndex = segmentIndex;
            }
            if (segmentOffset + chunk.compressedLength() > loadedSegment.length)
                throw new IllegalStateException("replay chunk lies outside segment " + segmentIndex);

            try (var arena = Arena.ofConfined()) {
                var compressed = arena.allocate(chunk.compressedLength());
                MemorySegment.copy(MemorySegment.ofArray(loadedSegment), segmentOffset,
                    compressed, 0, chunk.compressedLength());

                var uncompressed = arena.allocate(chunk.uncompressedLength());
                var uncompressedLength = Zstd.decompressUnsafe(uncompressed.address(), uncompressed.byteSize(),
                    compressed.address(), compressed.byteSize());
                if (Zstd.isError(uncompressedLength))
                    throw new IllegalStateException("Replay decompression failed: " + Zstd.getErrorName(uncompressedLength));
                if (uncompressedLength != chunk.uncompressedLength()) {
                    throw new IllegalStateException("Replay decompression length mismatch: expected "
                        + chunk.uncompressedLength() + ", got " + uncompressedLength);
                }

                visitor.chunk(chunk, NetworkBuffer.wrap(uncompressed, 0, uncompressedLength));
            }
        }

        visitor.close();
    }

    /// Decodes every event, which upgrades it, and writes it again at the current versions.
    private static final class Transcoded implements ReplayVisitor {
        private final ReplayVisitor downstream;
        private final ReplayEventRegistry registry;
        private final NetworkBuffer rewritten = NetworkBuffer.resizableBuffer();

        Transcoded(ReplayVisitor downstream, ReplayEventRegistry registry) {
            this.downstream = downstream;
            this.registry = registry;
        }

        @Override
        public void open(ReplayHeader header, CompoundBinaryTag metadata, int chunkCount) {
            downstream.open(header, metadata, chunkCount);
        }

        @Override
        public void chunk(ChunkIndex source, NetworkBuffer decoded) {
            rewritten.clear();
            for (var tick = 0; tick < source.tickCount(); tick++) {
                var tickIndex = decoded.read(NetworkBuffer.VAR_INT);
                if (tickIndex != source.startTick() + tick)
                    throw new IllegalStateException("replay tick does not match its chunk index");
                var events = decoded.read(NetworkBuffer.SHORT);
                if (events < 0) throw new IllegalStateException("negative replay event count");
                rewritten.write(NetworkBuffer.VAR_INT, tickIndex);
                rewritten.write(NetworkBuffer.SHORT, events);
                for (var event = 0; event < events; event++)
                    registry.write(rewritten, registry.read(decoded, source));
            }
            if (decoded.readableBytes() != 0)
                throw new IllegalStateException("replay chunk has bytes after its last tick");

            downstream.chunk(new ChunkIndex(source.startTick(), source.tickCount(), source.flags(),
                source.byteOffset(), source.compressedLength(), (int) rewritten.readableBytes()), rewritten);
        }

        @Override
        public void close() {
            downstream.close();
        }
    }

    /// Every chunk compressed alone into one segment, at the recorder's level.
    private static final class RecordedChunks implements ReplayVisitor {
        private final long segmentBase;
        private final NetworkBuffer segment = NetworkBuffer.resizableBuffer();
        private final List<ChunkIndex> index = new ArrayList<>();

        RecordedChunks(int segmentIndex) {
            this.segmentBase = ((long) segmentIndex) << 32;
        }

        @Override
        public void chunk(ChunkIndex source, NetworkBuffer decoded) {
            var dataLength = decoded.readableBytes();
            try (var arena = Arena.ofConfined()) {
                var raw = arena.allocate(dataLength);
                decoded.copyTo(decoded.readIndex(), raw, 0, dataLength);

                var compressed = arena.allocate(Zstd.compressBound(dataLength));
                var compressedLength = Zstd.compressUnsafe(compressed.address(), compressed.byteSize(),
                    raw.address(), dataLength, ReplayHeader.RECORD_COMPRESSION_LEVEL);
                if (Zstd.isError(compressedLength))
                    throw new IllegalStateException("Replay compression failed: " + Zstd.getErrorName(compressedLength));

                var offset = segment.writeIndex();
                segment.ensureWritable(compressedLength);
                NetworkBuffer.copy(NetworkBuffer.wrap(compressed, 0, compressedLength), 0,
                    segment, offset, compressedLength);
                segment.advanceWrite(compressedLength);
                index.add(new ChunkIndex(source.startTick(), source.tickCount(), source.flags(),
                    segmentBase | offset, (int) compressedLength, (int) dataLength));
            }
        }
    }

    /// Merges consecutive chunks into frames of at most [#FRAME_BYTE_LIMIT] raw bytes, recompressing
    /// each frame once.
    ///
    /// Only a frame starting at a source snapshot can be used as a seek anchor. Continuations of
    /// oversized chunks retain their tick bytes but must rebuild from a preceding snapshot.
    private static final class MergedFrames implements ReplayVisitor {
        private final NetworkBuffer chunks;
        private final NetworkBuffer pending;
        private final List<ChunkIndex> index = new ArrayList<>();
        private final int frameByteLimit;
        private final ReplayEventRegistry registry;

        private int startTick;
        private int tickCount;
        private byte flags;
        private @Nullable ChunkIndex versions;
        private boolean open;

        MergedFrames(int frameByteLimit, ReplayEventRegistry registry) {
            this.frameByteLimit = frameByteLimit;
            this.registry = registry;
            this.chunks = NetworkBuffer.resizableBuffer(frameByteLimit);
            this.pending = NetworkBuffer.resizableBuffer(frameByteLimit);
        }

        @Override
        public void open(ReplayHeader header, CompoundBinaryTag metadata, int chunkCount) {
            index.clear();
        }

        @Override
        public void chunk(ChunkIndex source, NetworkBuffer decoded) {
            if (decoded.readableBytes() <= frameByteLimit) {
                append(source.startTick(), source.tickCount(), source.flags(), source, decoded,
                    decoded.readIndex(), decoded.readableBytes());
                return;
            }

            for (var tick = 0; tick < source.tickCount(); tick++) {
                var offset = decoded.readIndex();
                var tickIndex = decoded.read(NetworkBuffer.VAR_INT);
                if (tickIndex != source.startTick() + tick)
                    throw new IllegalStateException("replay tick does not match its chunk index");
                var events = decoded.read(NetworkBuffer.SHORT);
                if (events < 0) throw new IllegalStateException("negative replay event count");
                for (var event = 0; event < events; event++) registry.read(decoded, source);
                var length = decoded.readIndex() - offset;
                if (length > frameByteLimit)
                    throw new IllegalStateException("replay tick " + tickIndex + " exceeds the frame limit: " + length);
                var flags = tick == 0 ? source.flags() : (byte) (source.flags() & ~ChunkIndex.FLAG_HAS_SNAPSHOT);
                append(tickIndex, 1, flags, source, decoded, offset, length);
            }
            if (decoded.readableBytes() != 0)
                throw new IllegalStateException("replay chunk has bytes after its last tick");
        }

        private void append(int tick, int count, byte sourceFlags, ChunkIndex source,
                            NetworkBuffer decoded, long offset, long length) {
            if (open && (pending.readableBytes() + length > frameByteLimit || !source.sameVersions(versions)))
                flush();
            if (!open) {
                startTick = tick;
                flags = sourceFlags;
                versions = source;
                tickCount = 0;
                open = true;
            }
            pending.ensureWritable(length);
            NetworkBuffer.copy(decoded, offset, pending, pending.writeIndex(), length);
            pending.advanceWrite(length);
            tickCount += count;
        }

        @Override
        public void close() {
            if (open) flush();
        }

        private void flush() {
            var dataLength = pending.readableBytes();
            var byteOffset = chunks.writeIndex();
            long compressedLength;
            try (var arena = Arena.ofConfined()) {
                var source = arena.allocate(dataLength);
                pending.copyTo(pending.readIndex(), source, 0, dataLength);

                var compressed = arena.allocate(Zstd.compressBound(dataLength));
                compressedLength = Zstd.compressUnsafe(compressed.address(), compressed.byteSize(),
                    source.address(), dataLength, ReplayHeader.COMPACT_COMPRESSION_LEVEL);
                if (Zstd.isError(compressedLength))
                    throw new IllegalStateException("Replay compression failed: " + Zstd.getErrorName(compressedLength));

                chunks.ensureWritable(compressedLength);
                NetworkBuffer.copy(NetworkBuffer.wrap(compressed, 0, compressedLength), 0,
                    chunks, byteOffset, compressedLength);
            }
            chunks.advanceWrite(compressedLength);

            // Offsets are relative to the first frame here; they become absolute once the preamble
            // in front of them has a length.
            index.add(new ChunkIndex(startTick, tickCount, flags, byteOffset, (int) compressedLength,
                (int) dataLength, versions.formatVersion(), versions.dataVersion()));
            pending.clear();
            open = false;
        }
    }
}
