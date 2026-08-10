package dev.hollowcube.replay.data;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/// The committed state of a segmented replay.
///
/// Segments referenced by this preamble are immutable. Appending always begins in a new segment,
/// then replaces the preamble after that segment has been written successfully.
public record ReplayPreamble(
    ReplayHeader header,
    CompoundBinaryTag metadata,
    List<ChunkIndex> index
) {

    public ReplayPreamble {
        index = List.copyOf(index);

        // The version needs no check here: a header is either built at the latest version or read
        // by a decoder that refuses anything else.
        if (header.dictionary() != ReplayDictionary.VERSION_LATEST)
            throw new IllegalArgumentException("unsupported replay dictionary: " + header.dictionary()
                + ", expected " + ReplayDictionary.VERSION_LATEST);
        if (header.tickCount() < 0)
            throw new IllegalArgumentException("negative replay tick count: " + header.tickCount());
        if (header.chunkCount() != index.size())
            throw new IllegalArgumentException("replay chunk count does not match index: header says "
                + header.chunkCount() + ", index holds " + index.size());

        int expectedTick = 0;
        int lastSegmentIndex = -1;
        long expectedSegmentOffset = 0;
        for (int i = 0; i < index.size(); i++) {
            var chunk = index.get(i);
            if (chunk.startTick() != expectedTick)
                throw new IllegalArgumentException("replay chunks are not tick-contiguous: chunk " + i
                    + " starts at tick " + chunk.startTick() + ", expected " + expectedTick);
            if (chunk.tickCount() <= 0)
                throw new IllegalArgumentException("replay chunk " + i + " has no ticks: " + chunk.tickCount());
            if (chunk.compressedLength() <= 0 || chunk.uncompressedLength() <= 0)
                throw new IllegalArgumentException("replay chunk " + i + " has an invalid length:"
                    + " compressed=" + chunk.compressedLength()
                    + ", uncompressed=" + chunk.uncompressedLength());

            expectedTick = Math.addExact(expectedTick, chunk.tickCount());

            int segmentIndex = segmentIndex(chunk);
            long segmentOffset = segmentOffset(chunk);
            if (segmentIndex < 0)
                throw new IllegalArgumentException("replay chunk " + i + " has a negative segment index: " + segmentIndex);
            if (segmentIndex < lastSegmentIndex || segmentIndex > lastSegmentIndex + 1)
                throw new IllegalArgumentException("replay segments are not contiguous: chunk " + i
                    + " is in segment " + segmentIndex + ", after segment " + lastSegmentIndex);
            if (segmentIndex != lastSegmentIndex) {
                if (segmentOffset != 0)
                    throw new IllegalArgumentException("first chunk in replay segment " + segmentIndex
                        + " has a non-zero offset: " + segmentOffset);
                lastSegmentIndex = segmentIndex;
                expectedSegmentOffset = 0;
            }
            if (segmentOffset != expectedSegmentOffset)
                throw new IllegalArgumentException("replay chunks are not segment-contiguous: chunk " + i
                    + " is at offset " + segmentOffset + ", expected " + expectedSegmentOffset);
            expectedSegmentOffset = Math.addExact(segmentOffset, chunk.compressedLength());
        }

        if (expectedTick != header.tickCount())
            throw new IllegalArgumentException("replay tick count does not match index: header says "
                + header.tickCount() + ", index holds " + expectedTick);
    }

    public static ReplayPreamble read(byte[] data) {
        try {
            if (data.length < ReplayHeader.HEADER_LENGTH)
                throw new IllegalArgumentException("truncated replay preamble: expected at least "
                    + ReplayHeader.HEADER_LENGTH + " bytes, got " + data.length);

            var buffer = NetworkBuffer.wrap(data, 0, data.length);
            var header = new ReplayHeader(buffer);

            if (header.metadataLength() < 0 || header.indexLength() < 0 || header.chunkCount() < 0)
                throw new IllegalArgumentException("replay preamble header contains negative lengths:"
                    + " metadata=" + header.metadataLength() + ", index=" + header.indexLength()
                    + ", chunks=" + header.chunkCount());
            long expectedLength = Math.addExact(
                ReplayHeader.HEADER_LENGTH,
                Math.addExact((long) header.metadataLength(), header.indexLength())
            );
            if (expectedLength != data.length)
                throw new IllegalArgumentException("replay preamble length does not match header:"
                    + " expected " + expectedLength + " bytes (header " + ReplayHeader.HEADER_LENGTH
                    + " + metadata " + header.metadataLength() + " + index " + header.indexLength()
                    + "), got " + data.length);

            long metadataStart = buffer.readIndex();
            var metadata = buffer.read(NetworkBuffer.NBT_COMPOUND);
            if (buffer.readIndex() - metadataStart != header.metadataLength())
                throw new IllegalArgumentException("replay metadata length does not match header:"
                    + " expected " + header.metadataLength() + " bytes, read "
                    + (buffer.readIndex() - metadataStart));

            long indexStart = buffer.readIndex();
            var index = new ArrayList<ChunkIndex>(header.chunkCount());
            for (int i = 0; i < header.chunkCount(); i++)
                index.add(buffer.read(ChunkIndex.NETWORK_TYPE));
            if (buffer.readIndex() - indexStart != header.indexLength())
                throw new IllegalArgumentException("replay index length does not match header:"
                    + " expected " + header.indexLength() + " bytes, read "
                    + (buffer.readIndex() - indexStart) + " for " + header.chunkCount() + " chunks");

            return new ReplayPreamble(header, metadata, index);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid replay preamble", e);
        }
    }

    public void requireCompatible(UUID worldId, byte[] worldVersion) {
        if (!header.worldId().equals(worldId))
            throw new IllegalArgumentException("replay belongs to a different world: " + header.worldId()
                + ", expected " + worldId);
        if (!Arrays.equals(header.worldVersion(), worldVersion))
            throw new IllegalArgumentException("replay belongs to a different version of world " + worldId);
    }

    public int nextSegmentIndex() {
        if (index.isEmpty()) return 0;
        return Math.addExact(segmentIndex(index.getLast()), 1);
    }

    public static int segmentIndex(ChunkIndex chunk) {
        return (int) (chunk.byteOffset() >>> 32);
    }

    public static long segmentOffset(ChunkIndex chunk) {
        return chunk.byteOffset() & 0xFFFFFFFFL;
    }
}
