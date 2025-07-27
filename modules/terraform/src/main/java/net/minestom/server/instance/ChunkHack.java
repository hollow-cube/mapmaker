package net.minestom.server.instance;

import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

// Exists here so that it can access package-private methods on Chunk
public final class ChunkHack {

    public static void invalidateChunk(@NotNull Chunk chunk) {
        ((DynamicChunk) chunk).chunkCache.invalidate();
    }

    /// Runs consumer for each tickable block in the chunk.
    /// The given point is absolute (ie world position not chunk local).
    public static void forEachTickable(@NotNull Chunk chunk, @NotNull BiConsumer<Point, Block> consumer) {
        ((DynamicChunk) chunk).tickableMap.int2ObjectEntrySet().fastForEach(entry -> {
            int x = chunk.getChunkX() * 16 + CoordConversion.chunkBlockIndexGetX(entry.getIntKey());
            int y = CoordConversion.chunkBlockIndexGetY(entry.getIntKey());
            int z = chunk.getChunkZ() * 16 + CoordConversion.chunkBlockIndexGetZ(entry.getIntKey());
            consumer.accept(new Vec(x, y, z), entry.getValue());
        });
    }
}
