package dev.hollowcube.replay.data;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.MagicConstant;

public record ChunkIndex(
    int startTick,
    int tickCount,
    @MagicConstant(flagsFromClass = ChunkIndex.class) byte flags,
    long byteOffset,
    int compressedLength,
    int uncompressedLength
) {
    public static final byte FLAG_HAS_SNAPSHOT = 0x1;

    public static final NetworkBuffer.Type<ChunkIndex> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, ChunkIndex::startTick,
        NetworkBuffer.VAR_INT, ChunkIndex::tickCount,
        NetworkBuffer.BYTE, ChunkIndex::flags,
        NetworkBuffer.LONG, ChunkIndex::byteOffset,
        NetworkBuffer.VAR_INT, ChunkIndex::compressedLength,
        NetworkBuffer.VAR_INT, ChunkIndex::uncompressedLength,
        ChunkIndex::new);

    /// True if this chunk contains a full snapshot, false otherwise.
    public boolean hasSnapshot() {
        return (flags & FLAG_HAS_SNAPSHOT) != 0;
    }

    public ChunkIndex withCompaction(long byteOffset, int compressedLength) {
        return new ChunkIndex(startTick, tickCount, flags, byteOffset, compressedLength, uncompressedLength);
    }
}
