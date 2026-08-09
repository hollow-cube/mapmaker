package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEventRegistry;
import dev.hollowcube.replay.io.SegmentedReplayCommit;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

final class ReplayRecorderImpl implements ReplayRecorder {
    private static final Executor WRITE_EXECUTOR = Thread::startVirtualThread;
    private static final byte[] NO_SEGMENT = new byte[0];

    /// Chunks bound seek granularity, so they stay small. Segments bound how often a commit reaches
    /// storage, which over the network is one request per commit, so they are much larger.
    private static final int CHUNK_TICK_LIMIT = 100;
    private static final int CHUNK_BYTE_LIMIT = 1024 * 1024;
    private static final int SEGMENT_BYTE_LIMIT = 4 * 1024 * 1024;

    private final ReplayEventRegistry registry;
    private final SegmentedReplayWriter writer;
    private final Runnable snapshot;

    private final ReplayHeader header;
    private final CompoundBinaryTag.Builder metadata;
    private final List<ChunkIndex> index;

    private final NetworkBuffer segmentBuffer;
    private final NetworkBuffer scratchBuffer;

    private int segmentIndex;
    private int lastChunkTick;
    private boolean committed;

    private long compressedBytes;
    private int totalEventCount;
    private int chunkEventCount;

    // Per-chunk snapshot state
    private boolean pendingSnapshot;
    private boolean chunkHasSnapshot;

    // Per-tick state
    private long tickStartPosition = -1;
    private long eventCountPosition = -1;
    private int eventCount = 0;

    private int tick = 0;

    private CompletableFuture<Void> writeChain = CompletableFuture.completedFuture(null);
    private CompletableFuture<Void> closeFuture;
    private volatile @Nullable Throwable failure;

    public ReplayRecorderImpl(
        ReplayEventRegistry registry,
        SegmentedReplayWriter writer,
        UUID worldId,
        byte[] worldVersion,
        Runnable snapshot
    ) {
        this(
            registry,
            writer,
            snapshot,
            new ReplayHeader(worldId, worldVersion),
            CompoundBinaryTag.empty(),
            List.of(),
            0,
            0,
            false
        );
    }

    public ReplayRecorderImpl(
        ReplayEventRegistry registry,
        SegmentedReplayWriter writer,
        ReplayPreamble preamble,
        Runnable snapshot
    ) {
        this(
            registry,
            writer,
            snapshot,
            preamble.header(),
            preamble.metadata(),
            preamble.index(),
            preamble.header().tickCount(),
            preamble.nextSegmentIndex(),
            true
        );
    }

    private ReplayRecorderImpl(
        ReplayEventRegistry registry,
        SegmentedReplayWriter writer,
        Runnable snapshot,
        ReplayHeader header,
        CompoundBinaryTag metadata,
        List<ChunkIndex> index,
        int tick,
        int segmentIndex,
        boolean committed
    ) {
        this.registry = registry;
        this.writer = writer;
        this.snapshot = snapshot;
        this.header = header;
        this.metadata = CompoundBinaryTag.builder().put(metadata);
        this.index = new ArrayList<>(index);
        this.tick = tick;
        this.lastChunkTick = tick;
        this.segmentIndex = segmentIndex;
        this.committed = committed;
        for (var chunkIndex : index) this.compressedBytes += chunkIndex.compressedLength();

        this.segmentBuffer = NetworkBuffer.resizableBuffer(16384); // TODO: figure out size
        this.scratchBuffer = NetworkBuffer.resizableBuffer(16384); // TODO: figure out a good size
        beginTick();
    }

    private void beginTick() {
        // The first tick of a chunk owes the reader a snapshot, because a seek to this chunk starts
        // decoding here with no knowledge of anything that came before it.
        if (tick == lastChunkTick) pendingSnapshot = true;

        tickStartPosition = scratchBuffer.writeIndex();
        scratchBuffer.write(NetworkBuffer.VAR_INT, tick);

        eventCountPosition = scratchBuffer.writeIndex();
        scratchBuffer.write(NetworkBuffer.SHORT, (short) 0 /* placeholder */);
    }

    private void endTick() {
        scratchBuffer.writeAt(eventCountPosition, NetworkBuffer.SHORT, (short) eventCount);

        this.eventCountPosition = -1;
        this.eventCount = 0;
        this.tickStartPosition = -1;
        this.tick++;
    }

    @Override
    public void advance() {
        ensureOpen();
        if (failure != null) return;
        captureSnapshot();

        endTick();

        if (tick - lastChunkTick >= CHUNK_TICK_LIMIT || scratchBuffer.readableBytes() >= CHUNK_BYTE_LIMIT)
            flushChunk();
        if (segmentBuffer.writeIndex() >= SEGMENT_BYTE_LIMIT)
            commit(false);

        beginTick();
        captureSnapshot();
    }

    @Override
    public void submit(ReplayEvent event) {
        ensureOpen();
        if (failure != null) return;
        captureSnapshot();

        // A tick record encodes its event count as a short, so more than Short.MAX_VALUE events in
        // one tick would silently corrupt the chunk.
        if (eventCount == Short.MAX_VALUE)
            throw new IllegalStateException("too many replay events in a single tick");

        this.registry.write(scratchBuffer, event);
        eventCount++;
        totalEventCount++;
        chunkEventCount++;
    }

    @Override
    public int tick() {
        return tick;
    }

    @Override
    public Stats stats() {
        return new Stats(tick, compressedBytes, index.size(), totalEventCount, chunkEventCount);
    }

    @Override
    public @Nullable Throwable failure() {
        return failure;
    }

    @Override
    public CompletableFuture<Void> flush() {
        ensureOpen();
        discardOpenTick();
        commit(false);
        beginTick();
        captureSnapshot();
        return writeChain;
    }

    /// Asks the host to describe the whole world at the start of a chunk's first tick, before that
    /// tick carries any of the host's own events.
    ///
    /// The host may of course submit nothing, in which case the chunk is still marked as carrying a
    /// snapshot: an empty description is a description.
    private void captureSnapshot() {
        if (!pendingSnapshot) return;

        // Cleared first, since the host submits through the same entry points that ask for this.
        pendingSnapshot = false;
        chunkHasSnapshot = true;
        snapshot.run();
    }

    @Override
    public CompletableFuture<Void> close() {
        return terminate(false);
    }

    @Override
    public CompletableFuture<Void> finish() {
        return terminate(true);
    }

    private CompletableFuture<Void> terminate(boolean finished) {
        if (closeFuture != null) return closeFuture;

        // advance() begins the next tick immediately. That tick is intentionally incomplete and
        // must not appear in the finished replay.
        discardOpenTick();
        commit(finished);

        closeFuture = writeChain.handleAsync((_, writeFailure) -> {
            Throwable failure = unwrapCompletionException(writeFailure);
            try {
                writer.close();
            } catch (Throwable closeFailure) {
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }

            if (failure != null) throw new CompletionException(failure);
            return null;
        }, WRITE_EXECUTOR);
        return closeFuture;
    }

    private void discardOpenTick() {
        scratchBuffer.writeIndex(tickStartPosition);
        tickStartPosition = -1;
        eventCountPosition = -1;
        totalEventCount -= eventCount;
        chunkEventCount -= eventCount;
        eventCount = 0;
    }

    /// Builds and dispatches one atomic commit carrying the complete preamble plus at most one new
    /// segment. Everything here runs on the caller's thread, so the commit is an immutable snapshot
    /// by the time it reaches the write chain.
    private void commit(boolean finished) {
        if (failure != null) return;
        if (tick > lastChunkTick) flushChunk();

        var hasSegment = segmentBuffer.writeIndex() > 0;
        // A final commit still has to reach storage to make the finished transition durable, but
        // only if this recording exists there at all.
        if (!hasSegment && !(finished && committed)) return;

        Integer commitSegmentIndex = null;
        var segment = NO_SEGMENT;
        if (hasSegment) {
            commitSegmentIndex = segmentIndex++;
            segment = segmentBuffer.read(NetworkBuffer.RAW_BYTES);
            segmentBuffer.clear();
        }

        var commit = new SegmentedReplayCommit(
            UUID.randomUUID(),
            buildPreamble(),
            commitSegmentIndex,
            segment,
            finished
        );
        committed = true;

        writeChain = writeChain
            .thenRunAsync(() -> writer.commit(commit), WRITE_EXECUTOR)
            .whenComplete((_, throwable) -> {
                if (throwable != null) failure = unwrapCompletionException(throwable);
            });
    }

    private byte[] buildPreamble() {
        return NetworkBuffer.makeArray(buffer -> {
            buffer.advanceWrite(ReplayHeader.HEADER_LENGTH); // we will patch later

            var writeIndex = buffer.writeIndex();
            buffer.write(NetworkBuffer.NBT_COMPOUND, this.metadata.build());
            var metadataLength = buffer.writeIndex() - writeIndex;

            writeIndex = buffer.writeIndex();
            for (var chunkIndex : this.index)
                buffer.write(ChunkIndex.NETWORK_TYPE, chunkIndex);
            var indexLength = buffer.writeIndex() - writeIndex;

            this.header.update((int) metadataLength, (int) indexLength, tick, this.index.size());
            writeIndex = buffer.writeIndex();
            buffer.writeIndex(0);
            this.header.write(buffer);
            buffer.writeIndex(writeIndex);
        });
    }

    private void flushChunk() {
        long dataLength = scratchBuffer.readableBytes();
        long requiredSize = Zstd.compressBound(dataLength);
        long compressedLength;
        try (var arena = Arena.ofConfined()) {
            var source = arena.allocate(dataLength);
            scratchBuffer.copyTo(scratchBuffer.readIndex(), source, 0, dataLength);

            var compressed = arena.allocate(requiredSize);
            compressedLength = Zstd.compressUnsafe(
                compressed.address(), requiredSize,
                source.address(), dataLength,
                ReplayHeader.RECORD_COMPRESSION_LEVEL
            );
            if (Zstd.isError(compressedLength)) {
                throw new IllegalStateException("Replay compression failed: " + Zstd.getErrorName(compressedLength));
            }

            segmentBuffer.ensureWritable(compressedLength);
            NetworkBuffer.copy(
                NetworkBuffer.wrap(compressed, 0, compressedLength),
                0,
                segmentBuffer,
                segmentBuffer.writeIndex(),
                compressedLength
            );
        }

        var index = new ChunkIndex(
            lastChunkTick,
            tick - lastChunkTick,
            chunkHasSnapshot ? ChunkIndex.FLAG_HAS_SNAPSHOT : 0,
            (((long) segmentIndex) << 32) | (segmentBuffer.writeIndex() & 0xFFFFFFFFL),
            (int) compressedLength,
            (int) dataLength
        );
        this.index.add(index);

        segmentBuffer.advanceWrite(compressedLength);
        scratchBuffer.clear();
        lastChunkTick = tick;
        chunkHasSnapshot = false;
        compressedBytes += compressedLength;
        chunkEventCount = 0;

        // todo should reuse the same zstd context here and for the initial write as a small optimization
    }

    private void ensureOpen() {
        if (closeFuture != null)
            throw new IllegalStateException("Replay recorder is closed");
    }

    private static Throwable unwrapCompletionException(Throwable throwable) {
        while (throwable instanceof CompletionException && throwable.getCause() != null)
            throwable = throwable.getCause();
        return throwable;
    }

}
