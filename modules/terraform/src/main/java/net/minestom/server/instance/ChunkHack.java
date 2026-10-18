package net.minestom.server.instance;

import org.jetbrains.annotations.NotNull;

// Exists here so that it can access package-private methods on Chunk
public final class ChunkHack {

    public static void invalidateChunk(@NotNull Chunk chunk) {
        // EmptyChunk should never be invalidated
        if (!(chunk instanceof DynamicChunk dynamicChunk)) return;
        dynamicChunk.chunkCache.invalidate();
    }
}
