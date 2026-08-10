package dev.hollowcube.replay.io;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Reads a compacted replay held entirely in memory.
///
/// Replays are small enough to hold whole for now. Chunk data is addressed by absolute offset, so
/// swapping this for a ranged remote read is a matter of fetching `compressedLength` bytes at
/// `byteOffset` instead of slicing the local segment.
public final class CompactedReplayReader implements ReplayReader {
    private final Arena arena;
    private final MemorySegment data;

    private final ReplayHeader header;
    private final CompoundBinaryTag metadata;
    private final List<ChunkIndex> index;

    private MemorySegment scratch = MemorySegment.NULL;

    public static CompactedReplayReader open(Path path) {
        try {
            return new CompactedReplayReader(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read replay " + path, e);
        }
    }

    public CompactedReplayReader(byte[] replay) {
        this.arena = Arena.ofShared();
        this.data = arena.allocate(replay.length);
        this.data.copyFrom(MemorySegment.ofArray(replay));

        var buffer = NetworkBuffer.wrap(data, 0, data.byteSize());
        this.header = new ReplayHeader(buffer);
        this.metadata = buffer.read(NetworkBuffer.NBT_COMPOUND);

        var index = new ArrayList<ChunkIndex>(header.chunkCount());
        for (var i = 0; i < header.chunkCount(); i++)
            index.add(buffer.read(ChunkIndex.NETWORK_TYPE));
        this.index = List.copyOf(index);
    }

    @Override
    public ReplayHeader header() {
        return header;
    }

    @Override
    public CompoundBinaryTag metadata() {
        return metadata;
    }

    @Override
    public List<ChunkIndex> index() {
        return index;
    }

    /// Never null: the whole replay is already in memory, so every chunk is resident.
    @Override
    public NetworkBuffer chunk(ChunkIndex chunk) {
        if (chunk.byteOffset() < 0 || chunk.byteOffset() + chunk.compressedLength() > data.byteSize())
            throw new IllegalArgumentException("replay chunk lies outside the replay");

        if (scratch.byteSize() < chunk.uncompressedLength())
            scratch = arena.allocate(chunk.uncompressedLength());

        var uncompressedLength = Zstd.decompressUnsafe(
            scratch.address(),
            chunk.uncompressedLength(),
            data.address() + chunk.byteOffset(),
            chunk.compressedLength()
        );
        if (Zstd.isError(uncompressedLength))
            throw new IllegalStateException("Replay decompression failed: " + Zstd.getErrorName(uncompressedLength));
        if (uncompressedLength != chunk.uncompressedLength()) {
            throw new IllegalStateException(
                "Replay decompression length mismatch: expected "
                    + chunk.uncompressedLength() + ", got " + uncompressedLength
            );
        }

        return NetworkBuffer.wrap(scratch, 0, uncompressedLength);
    }

    @Override
    public void prefetch(ChunkIndex chunk) {
        // Nothing to fetch.
    }

    @Override
    public void close() {
        arena.close();
    }
}
