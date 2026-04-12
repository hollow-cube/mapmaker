package dev.hollowcube.replay.data;

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

    /// True if this chunk contains a full snapshot, false otherwise.
    public boolean hasSnapshot() {
        return (flags & FLAG_HAS_SNAPSHOT) != 0;
    }
}
