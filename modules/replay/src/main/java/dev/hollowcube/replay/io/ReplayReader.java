package dev.hollowcube.replay.io;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.List;

/// An open compacted replay. Implementations own where the bytes come from.
///
/// Only compacted replays are readable. A segmented recording's chunk offsets are relative to a
/// segment rather than to the replay as a whole, so it has to be compacted before playback.
public interface ReplayReader extends Closeable {

    ReplayHeader header();

    CompoundBinaryTag metadata();

    List<ChunkIndex> index();

    /// The decoded chunk if it is resident, or null if it is not and has to be fetched first.
    ///
    /// Never blocks and never fetches on its own; ask with [#prefetch] and try again later. The
    /// returned buffer holds exactly that chunk's tick records, and is only valid until the next
    /// call.
    @Nullable NetworkBuffer chunk(ChunkIndex chunk);

    /// Asks for a chunk to be made resident, returning immediately. Only a hint; whether it is
    /// honoured, cached or evicted again is entirely the reader's business.
    void prefetch(ChunkIndex chunk);

    @Override
    void close();
}
