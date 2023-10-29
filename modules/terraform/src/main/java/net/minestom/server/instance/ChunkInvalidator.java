package net.minestom.server.instance;

import org.jetbrains.annotations.NotNull;

public final class ChunkInvalidator {

    public static void invalidateChunk(@NotNull Chunk chunk) {
        ((DynamicChunk) chunk).chunkCache.invalidate();
    }
}
