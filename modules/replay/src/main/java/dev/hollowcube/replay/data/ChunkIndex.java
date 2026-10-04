package dev.hollowcube.replay.data;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;
import org.intellij.lang.annotations.MagicConstant;

/// @param formatVersion the [ReplayHeader#VERSION_LATEST] of the build that wrote the chunk, per
///                      chunk because a resumed recording mixes chunks from before and after an
///                      upgrade
/// @param dataVersion   the [MinecraftServer#DATA_VERSION] of the build that wrote the chunk
public record ChunkIndex(
    int startTick,
    int tickCount,
    @MagicConstant(flagsFromClass = ChunkIndex.class) byte flags,
    long byteOffset,
    int compressedLength,
    int uncompressedLength,
    int formatVersion,
    int dataVersion
) {
    public static final byte FLAG_HAS_SNAPSHOT = 0x1;

    public ChunkIndex(int startTick, int tickCount, byte flags, long byteOffset, int compressedLength, int uncompressedLength) {
        this(startTick, tickCount, flags, byteOffset, compressedLength, uncompressedLength,
            ReplayHeader.VERSION_LATEST, MinecraftServer.DATA_VERSION);
    }

    public static ChunkIndex read(NetworkBuffer buffer) {
        var startTick = buffer.read(NetworkBuffer.VAR_INT);
        var tickCount = buffer.read(NetworkBuffer.VAR_INT);
        var flags = buffer.read(NetworkBuffer.BYTE);
        var byteOffset = buffer.read(NetworkBuffer.LONG);
        var compressedLength = buffer.read(NetworkBuffer.VAR_INT);
        var uncompressedLength = buffer.read(NetworkBuffer.VAR_INT);
        return new ChunkIndex(startTick, tickCount, flags, byteOffset, compressedLength, uncompressedLength,
            buffer.read(NetworkBuffer.VAR_INT), buffer.read(NetworkBuffer.VAR_INT));
    }

    public void write(NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, startTick);
        buffer.write(NetworkBuffer.VAR_INT, tickCount);
        buffer.write(NetworkBuffer.BYTE, flags);
        buffer.write(NetworkBuffer.LONG, byteOffset);
        buffer.write(NetworkBuffer.VAR_INT, compressedLength);
        buffer.write(NetworkBuffer.VAR_INT, uncompressedLength);
        buffer.write(NetworkBuffer.VAR_INT, formatVersion);
        buffer.write(NetworkBuffer.VAR_INT, dataVersion);
    }

    /// True if this chunk contains a full snapshot, false otherwise.
    public boolean hasSnapshot() {
        return (flags & FLAG_HAS_SNAPSHOT) != 0;
    }

    public boolean sameVersions(ChunkIndex other) {
        return formatVersion == other.formatVersion && dataVersion == other.dataVersion;
    }

    public ChunkIndex withCompaction(long byteOffset, int compressedLength) {
        return new ChunkIndex(startTick, tickCount, flags, byteOffset, compressedLength, uncompressedLength,
            formatVersion, dataVersion);
    }
}
