package net.hollowcube.anticheat.capture;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.hollowcube.anticheat.log.TraceWorld;
import net.hollowcube.anticheat.log.WorldChunk;
import net.hollowcube.anticheat.protocol.Positions;
import net.hollowcube.anticheat.world.Chunk;
import net.hollowcube.anticheat.world.WorldView;

import java.util.*;

/// The chunks a capture cared about, accumulated as it runs.
///
/// The connection notes a chunk here every time the player is in it and every time a tracked entity
/// close enough to matter is in it, against the time it happened, because the two things that ask
/// for a region — the capture's own stop and a ring flush — start at different points and each
/// wants only what happened after its own. Positions are kept as chunks, not coordinates: the
/// region is chunks in the end, and a session's worth of them is a few hundred longs.
///
/// Owned by the connection's event loop; the region it produces is handed to the writer thread as a
/// plain set.
public final class Trim {

    /// Primitive rather than `Map<Long, Long>` because [#add] runs on every move packet, where a
    /// boxed merge allocates a key and a value per call.
    private final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();

    public Trim() {
        chunks.defaultReturnValue(Long.MIN_VALUE);
    }

    public void add(long tNs, double x, double z) {
        long pos = Positions.chunkPos((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        if (tNs > chunks.get(pos)) chunks.put(pos, tNs);
    }

    public Set<Long> since(long tNs) {
        var result = new LongOpenHashSet();
        for (var entry : chunks.long2LongEntrySet())
            if (entry.getLongValue() >= tNs) result.add(entry.getLongKey());
        return result;
    }

    /// Forgets chunks nothing can ask for any more — anything older than the earliest point a
    /// trace could still start at.
    public void prune(long beforeNs) {
        for (var iterator = chunks.values().iterator(); iterator.hasNext(); )
            if (iterator.nextLong() < beforeNs) iterator.remove();
    }

    public int size() {
        return chunks.size();
    }

    /// The chunks a trace keeps: every loaded chunk within the policy's radius of a noted one.
    /// Positions that were never loaded, or have since been forgotten, contribute nothing.
    public static Set<Long> region(WorldView world, TrimPolicy policy, Set<Long> centers) {
        if (policy.keepsEverything()) return Set.copyOf(world.chunks().keySet());

        var kept = new HashSet<Long>();
        int radius = policy.chunkRadius();
        for (long center : centers) {
            int centerX = Positions.chunkX(center);
            int centerZ = Positions.chunkZ(center);
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    long pos = Positions.chunkPos(x, z);
                    if (world.chunks().containsKey(pos)) kept.add(pos);
                }
            }
        }
        return kept;
    }

    /// The trimmed world in the form the writer takes it, in a fixed order so two runs of the same
    /// capture produce the same bytes.
    public static TraceWorld world(WorldView world, TrimPolicy policy, Set<Long> centers) {
        var kept = new ArrayList<Chunk>();
        for (long pos : region(world, policy, centers)) {
            var chunk = world.chunk(Positions.chunkX(pos), Positions.chunkZ(pos));
            if (chunk != null) kept.add(chunk);
        }
        kept.sort(Comparator.comparingInt(Chunk::chunkX).thenComparingInt(Chunk::chunkZ));

        var chunks = new ArrayList<WorldChunk>(kept.size());
        for (var chunk : kept) {
            var sections = new ArrayList<WorldChunk.SectionEntry>(chunk.sectionCount());
            for (var section : chunk.sections()) sections.add(new WorldChunk.SectionEntry.Inline(section));
            chunks.add(new WorldChunk(chunk.chunkX(), chunk.chunkZ(), sections));
        }
        var result = List.copyOf(chunks);
        return () -> result;
    }
}
