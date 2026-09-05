package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.io.ReplayReader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;

import java.util.List;

/// One pass over a replay's chunks, decoded and in recorded order.
///
/// **A visitor may not depend on being driven by a compaction.** [#walk] drives the same interface
/// over an already-compacted replay, and the two disagree on where the boundaries fall —
/// compaction shows the recorded chunks, a walk over its result shows the merged frames. They agree
/// on everything a reader of the ticks can see: the buffers concatenate to the same bytes in the
/// same order. Accumulating across chunks is fine; treating a boundary as meaningful is not.
///
/// The buffer handed to [#chunk] is valid only for that call — it wraps scratch memory the next
/// chunk reuses.
public interface ReplayVisitor {

    default void open(ReplayHeader header, CompoundBinaryTag metadata, int chunkCount) {
    }

    /// @param source the entry these bytes were stored under: its `startTick`, `tickCount` and
    ///               snapshot flag describe them, its offsets do not
    void chunk(ChunkIndex source, NetworkBuffer decoded);

    default void close() {
    }

    /// The other half of the promise above: an indexer written for a compaction re-runs over the
    /// object that compaction produced. The reader must have every chunk resident, which a
    /// whole-file one does.
    static void walk(ReplayReader reader, ReplayVisitor visitor) {
        visitor.open(reader.header(), reader.metadata(), reader.index().size());
        for (var chunk : reader.index()) {
            var decoded = reader.chunk(chunk);
            if (decoded == null)
                throw new IllegalStateException("replay chunk at tick " + chunk.startTick() + " is not resident");
            visitor.chunk(chunk, decoded);
        }
        visitor.close();
    }

    /// Every one of `visitors`, in order, over one pass.
    static ReplayVisitor of(List<ReplayVisitor> visitors) {
        return new ReplayVisitor() {
            @Override
            public void open(ReplayHeader header, CompoundBinaryTag metadata, int chunkCount) {
                for (var visitor : visitors) visitor.open(header, metadata, chunkCount);
            }

            @Override
            public void chunk(ChunkIndex source, NetworkBuffer decoded) {
                for (var visitor : visitors) {
                    // The one before it left the cursor at the end.
                    decoded.readIndex(0);
                    visitor.chunk(source, decoded);
                }
            }

            @Override
            public void close() {
                for (var visitor : visitors) visitor.close();
            }
        };
    }
}
