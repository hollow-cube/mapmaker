package net.hollowcube.anticheat.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.anticheat.protocol.*;
import org.jetbrains.annotations.Nullable;

/// The client's chunk cache, as a map from `ChunkPos#pack` to [Chunk].
///
/// `ClientChunkCache` stores chunks in a ring of `(2r+1)^2` slots with
/// `r = max(2, serverViewDistance) + 3` and reads through `inRange`, so a chunk outside the ring is
/// invisible to the client even though its slot still holds it. This keeps a plain map instead and
/// drops out-of-range chunks when the centre or the radius moves, which is what the client's reads
/// see; the one case the two models disagree on is a centre that moves away from a chunk and back
/// without the server resending or forgetting it, which the server's own tracking does not produce.
///
/// All state is owned by the calling thread; snapshots are the only thing that leaves it and they
/// are immutable.
public final class ChunkMap {

    private final DimensionRegistry dimensions = new DimensionRegistry();

    private Long2ObjectMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
    private boolean chunksShared;
    private int generation;

    private DimensionInfo dimension = DimensionInfo.OVERWORLD;
    private String level = "";
    private int viewCenterX;
    private int viewCenterZ;
    private int storageRadius = storageRadius(0);

    /// `ClientChunkCache#calculateStorageRange`.
    private static int storageRadius(int serverViewDistance) {
        return Math.max(2, serverViewDistance) + 3;
    }

    public int chunkCount() {
        return chunks.size();
    }

    public DimensionInfo dimension() {
        return dimension;
    }

    public int viewCenterX() {
        return viewCenterX;
    }

    public int viewCenterZ() {
        return viewCenterZ;
    }

    public int storageRadius() {
        return storageRadius;
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

    /// An immutable view of the world right now. Constant time: the view keeps this map's storage
    /// and bumping the generation makes every chunk in it copy-on-write.
    public WorldView snapshot() {
        chunksShared = true;
        generation++;
        return new WorldView(Long2ObjectMaps.unmodifiable(chunks), dimension,
            viewCenterX, viewCenterZ, storageRadius);
    }

    /// Applies whatever of a decoded packet the world model cares about, ignoring the rest.
    public void handle(Packet packet) {
        switch (packet) {
            case S2CRegistryData registryData -> apply(registryData);
            case S2CLogin login -> apply(login);
            case S2CRespawn respawn -> apply(respawn);
            case S2CStartConfiguration startConfiguration -> apply(startConfiguration);
            case S2CLevelChunkWithLight levelChunk -> apply(levelChunk);
            case S2CBlockUpdate blockUpdate -> apply(blockUpdate);
            case S2CSectionBlocksUpdate sectionBlocksUpdate -> apply(sectionBlocksUpdate);
            case S2CForgetLevelChunk forgetChunk -> apply(forgetChunk);
            case S2CSetChunkCacheCenter chunkCacheCenter -> apply(chunkCacheCenter);
            case S2CSetChunkCacheRadius chunkCacheRadius -> apply(chunkCacheRadius);
            // Everything else is somebody else's state; the world model only follows the packets
            // that build, edit or drop the client's level.
            case null, default -> {
            }
        }
    }

    public void apply(S2CRegistryData packet) {
        dimensions.apply(packet);
    }

    /// `handleLogin` always builds a new `ClientLevel` and a new `ClientChunkCache`, so everything
    /// goes, including the view centre.
    public void apply(S2CLogin packet) {
        clearChunks();
        dimension = dimensions.get(packet.spawnInfo().dimensionTypeId());
        level = packet.spawnInfo().dimension();
        viewCenterX = 0;
        viewCenterZ = 0;
        storageRadius = storageRadius(packet.chunkRadius());
    }

    /// `handleRespawn` only builds a new `ClientLevel` when the dimension changes; respawning in
    /// place keeps every loaded chunk.
    public void apply(S2CRespawn packet) {
        var spawnInfo = packet.spawnInfo();
        if (spawnInfo.dimension().equals(level)) return;
        clearChunks();
        dimension = dimensions.get(spawnInfo.dimensionTypeId());
        level = spawnInfo.dimension();
        viewCenterX = 0;
        viewCenterZ = 0;
    }

    /// `handleConfigurationStart` calls `clearClientLevel`: the level and the registries are gone
    /// and the next configuration phase sends its own.
    public void apply(S2CStartConfiguration packet) {
        clearChunks();
        dimensions.clear();
        dimension = DimensionInfo.OVERWORLD;
        level = "";
        viewCenterX = 0;
        viewCenterZ = 0;
        storageRadius = storageRadius(0);
    }

    /// `ClientChunkCache#replaceWithPacketData` ignores a chunk outside the ring, so this does too.
    public void apply(S2CLevelChunkWithLight packet) {
        if (!inRange(packet.chunkX(), packet.chunkZ())) return;
        chunks().put(Positions.chunkPos(packet.chunkX(), packet.chunkZ()),
            Chunk.of(packet, dimension, generation));
    }

    public void apply(S2CBlockUpdate packet) {
        long pos = packet.packedPos();
        setBlockState(Positions.blockX(pos), Positions.blockY(pos), Positions.blockZ(pos), packet.blockStateId());
    }

    public void apply(S2CSectionBlocksUpdate packet) {
        long section = packet.packedSectionPos();
        int baseX = Positions.sectionX(section) << 4;
        int baseY = Positions.sectionY(section) << 4;
        int baseZ = Positions.sectionZ(section) << 4;
        for (long entry : packet.entries())
            setBlockState(baseX + S2CSectionBlocksUpdate.relativeX(entry),
                baseY + S2CSectionBlocksUpdate.relativeY(entry),
                baseZ + S2CSectionBlocksUpdate.relativeZ(entry),
                S2CSectionBlocksUpdate.blockStateId(entry));
    }

    public void apply(S2CForgetLevelChunk packet) {
        long pos = Positions.chunkPos(packet.chunkX(), packet.chunkZ());
        if (chunks.containsKey(pos)) chunks().remove(pos);
    }

    public void apply(S2CSetChunkCacheCenter packet) {
        viewCenterX = packet.chunkX();
        viewCenterZ = packet.chunkZ();
        dropOutOfRange();
    }

    /// `updateViewRadius` only rebuilds its storage when the derived range changes, and copies over
    /// the chunks the new range still covers.
    public void apply(S2CSetChunkCacheRadius packet) {
        int radius = storageRadius(packet.radius());
        if (radius == storageRadius) return;
        storageRadius = radius;
        dropOutOfRange();
    }

    private void setBlockState(int x, int y, int z, int stateId) {
        var chunk = mutableChunk(x >> 4, z >> 4);
        if (chunk != null) chunk.setBlockState(x & 0xF, y, z & 0xF, stateId);
    }

    private @Nullable Chunk mutableChunk(int chunkX, int chunkZ) {
        long pos = Positions.chunkPos(chunkX, chunkZ);
        var chunk = chunks.get(pos);
        if (chunk == null) return null;
        if (chunk.generation() == generation) return chunk;

        var copy = chunk.copy(generation);
        chunks().put(pos, copy);
        return copy;
    }

    private boolean inRange(int chunkX, int chunkZ) {
        return Math.abs(chunkX - viewCenterX) <= storageRadius
            && Math.abs(chunkZ - viewCenterZ) <= storageRadius;
    }

    private void dropOutOfRange() {
        var dropped = new LongArrayList();
        for (long pos : chunks.keySet())
            if (!inRange(Positions.chunkX(pos), Positions.chunkZ(pos))) dropped.add(pos);
        if (dropped.isEmpty()) return;
        var chunks = chunks();
        for (long pos : dropped) chunks.remove(pos);
    }

    private void clearChunks() {
        if (chunks.isEmpty()) return;
        chunks = new Long2ObjectOpenHashMap<>();
        chunksShared = false;
    }

    private Long2ObjectMap<Chunk> chunks() {
        if (chunksShared) {
            chunks = new Long2ObjectOpenHashMap<>(chunks);
            chunksShared = false;
        }
        return chunks;
    }
}
