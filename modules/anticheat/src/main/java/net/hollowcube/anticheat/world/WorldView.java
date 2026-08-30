package net.hollowcube.anticheat.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.anticheat.protocol.Positions;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/// An immutable view of a [ChunkMap] at one instant, as taken by a snapshot.
///
/// The chunks are the live objects, not copies: the map hands the view its own storage and copies
/// on the next write instead (see [Chunk]), so taking a view costs nothing.
public record WorldView(
    Long2ObjectMap<Chunk> chunks,
    DimensionInfo dimension,
    int viewCenterX,
    int viewCenterZ,
    int storageRadius
) {

    public int chunkCount() {
        return chunks.size();
    }

    public Collection<Chunk> loadedChunks() {
        return chunks.values();
    }

    public @Nullable Chunk chunk(int chunkX, int chunkZ) {
        return chunks.get(Positions.chunkPos(chunkX, chunkZ));
    }

    /// The global block state id at an absolute block position, or -1 when the chunk is not loaded
    /// or the position is outside build height.
    public int blockState(int x, int y, int z) {
        var chunk = chunk(x >> 4, z >> 4);
        return chunk == null ? -1 : chunk.blockState(x & 0xF, y, z & 0xF);
    }
}
