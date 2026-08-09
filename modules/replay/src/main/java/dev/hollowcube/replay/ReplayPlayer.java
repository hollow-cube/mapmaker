package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEventRegistry;
import dev.hollowcube.replay.io.ReplayReader;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/// Decodes a compacted replay one tick at a time.
///
/// This deliberately owns no scheduler and no notion of speed or pausing. Hosts already have a
/// tick loop with its own idea of timing, so they call [#advance()] when they want the next tick
/// and stop calling it when they don't.
///
/// Nothing here blocks. A reader whose chunks are not all resident makes [#advance()] report
/// [Advance#STALLED] instead, and playback simply does not move until the data arrives.
public final class ReplayPlayer implements AutoCloseable {
    /// The outcome of one [#advance()].
    public enum Advance {
        /// A tick was played.
        ADVANCED,
        /// The chunk holding the next tick is not resident yet. Nothing was played and the
        /// position did not move; the reader has been asked for it.
        STALLED,
        /// The replay is over.
        FINISHED
    }

    private final ReplayReader reader;
    private final ReplayEventRegistry registry;
    private final Consumer<ReplayEvent> handler;

    private int chunkIndex = -1;
    private @Nullable NetworkBuffer chunk;
    private int tick;

    /// The tick a pending [#seek(int)] is heading for, or -1 when there is none, and the tick that
    /// reconstruction toward it has reached.
    private int seekTarget = -1;
    private int seekCursor;

    public ReplayPlayer(ReplayReader reader, ReplayEventRegistry registry, Consumer<ReplayEvent> handler) {
        this.reader = reader;
        this.registry = registry;
        this.handler = handler;
    }

    /// The tick that the next [#advance()] will play. A pending seek reports its target, since
    /// that is where playback is going and the ticks in between are only replayed to rebuild state.
    public int tick() {
        return tick;
    }

    public int tickCount() {
        return reader.header().tickCount();
    }

    /// Plays the next tick, dispatching its events in recorded order.
    ///
    /// Once [Advance#FINISHED] further calls do nothing. [Advance#STALLED] is not terminal: the
    /// caller should try again later, and must not treat it as time having passed.
    public Advance advance() {
        if (seekTarget != -1 && !reconstruct()) return Advance.STALLED;
        if (tick >= tickCount()) return Advance.FINISHED;

        var buffer = chunkContaining(tick);
        if (buffer == null) return Advance.STALLED;

        playTick(buffer);
        tick++;
        return Advance.ADVANCED;
    }

    /// Moves playback to a tick, replaying forward from the nearest preceding chunk that opens with
    /// a snapshot, so that entity state is rebuilt rather than left wherever the previous position
    /// happened to leave it.
    ///
    /// A recorder asks its host for a snapshot at the start of every chunk, so that is normally the
    /// chunk containing the target tick. A chunk without one is skipped over rather than trusted,
    /// which costs replaying the ticks in between; if no preceding chunk has a snapshot at all
    /// (a recording made before snapshots existed) this replays from the very beginning.
    ///
    /// Rebuilding happens here when the chunks it needs are resident, and is otherwise left to
    /// [#advance()], which reports [Advance#STALLED] until they are. Either way [#tick()] reads as
    /// the target immediately, so the position a host shows is the one it asked for even while the
    /// state behind it is still being fetched.
    public void seek(int tick) {
        if (tick < 0 || tick > tickCount())
            throw new IllegalArgumentException("cannot seek to tick " + tick + " of " + tickCount());

        this.tick = tick;
        this.seekTarget = -1;
        if (tick == tickCount()) return;

        var anchor = chunkIndexContaining(tick);
        while (anchor > 0 && !reader.index().get(anchor).hasSnapshot()) anchor--;

        // The cached chunk is mid-read, so it cannot be reused even if it is the anchor.
        this.chunkIndex = -1;
        this.chunk = null;
        this.seekTarget = tick;
        this.seekCursor = reader.index().get(anchor).startTick();

        // Rebuilds now if the anchor is already resident, and asks for it if it is not.
        reconstruct();
    }

    @Override
    public void close() {
        reader.close();
    }

    /// Replays from the seek anchor up to the target, returning false if it ran out of resident
    /// chunks on the way. Progress made is kept, so a later call resumes rather than restarts.
    private boolean reconstruct() {
        while (seekCursor < seekTarget) {
            var buffer = chunkContaining(seekCursor);
            if (buffer == null) return false;

            playTick(buffer);
            seekCursor++;
        }
        seekTarget = -1;
        return true;
    }

    private void playTick(NetworkBuffer buffer) {
        buffer.read(NetworkBuffer.VAR_INT); // tick index, implied by our position in the chunk
        var eventCount = buffer.read(NetworkBuffer.SHORT);
        for (var i = 0; i < eventCount; i++)
            handler.accept(registry.read(buffer));
    }

    private @Nullable NetworkBuffer chunkContaining(int tick) {
        var required = chunkIndexContaining(tick);
        if (chunk == null || required != chunkIndex) {
            var buffer = reader.chunk(reader.index().get(required));
            if (buffer == null) {
                reader.prefetch(reader.index().get(required));
                return null;
            }

            chunkIndex = required;
            chunk = buffer;

            // Entering a chunk is the last moment the next one can be asked for without playback
            // having to wait on it, and sequential play is the common case.
            if (required + 1 < reader.index().size())
                reader.prefetch(reader.index().get(required + 1));
        }
        return chunk;
    }

    private int chunkIndexContaining(int tick) {
        var index = reader.index();

        // Chunks are tick-contiguous and ordered, so a binary search finds the owner directly.
        var low = 0;
        var high = index.size() - 1;
        while (low <= high) {
            var middle = (low + high) >>> 1;
            ChunkIndex chunk = index.get(middle);
            if (tick < chunk.startTick()) high = middle - 1;
            else if (tick >= chunk.startTick() + chunk.tickCount()) low = middle + 1;
            else return middle;
        }
        throw new IllegalStateException("no replay chunk contains tick " + tick);
    }
}
