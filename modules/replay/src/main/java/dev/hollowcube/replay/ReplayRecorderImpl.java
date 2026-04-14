package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.ReplayEvent;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.minestom.server.network.PolarBufferAccessWidener.networkBufferAddress;

/// ReplayRecorder represents an active recording in progress.
///
/// It may be created from scratch, or resumed from a previous partial recording.
final class ReplayRecorderImpl implements ReplayRecorder {

    private final ReplayHeader header;
    private final CompoundBinaryTag.Builder metadata;
    private final List<ChunkIndex> index;

    private final NetworkBuffer segmentBuffer;
    private final NetworkBuffer scratchBuffer;

    private int segmentIndex = 0;
    private int lastFlushTick = 0;

    // Per-tick state
    private long eventCountPosition = -1;
    private int eventCount = 0;

    private int tick = 0;

    public ReplayRecorderImpl(UUID worldId, UUID worldVersion) {
        this.header = new ReplayHeader(worldId, worldVersion);
        this.metadata = CompoundBinaryTag.builder();
        this.index = new ArrayList<>();

        this.segmentBuffer = NetworkBuffer.resizableBuffer(16384); // TODO: figure out size
        this.scratchBuffer = NetworkBuffer.resizableBuffer(16384); // TODO: figure out a good size
        beginTick();
    }

    private void beginTick() {
        scratchBuffer.write(NetworkBuffer.VAR_INT, tick);

        eventCountPosition = scratchBuffer.writeIndex();
        scratchBuffer.write(NetworkBuffer.SHORT, (short) 0 /* placeholder */);
    }

    private void endTick() {
        scratchBuffer.writeAt(eventCountPosition, NetworkBuffer.SHORT, (short) eventCount);
        System.out.println("patched " + eventCount + " events");

        this.eventCountPosition = -1;
        this.eventCount = 0;
        this.tick++;
    }

    @Override
    public void advance() {
        endTick();
        System.out.println("REPLAY: tick " + tick);

        if (shouldFlush()) flush();

        beginTick();
    }

    @Override
    public void submit(ReplayEvent event) {
        System.out.println("REPLAY: recording " + event.getClass().getSimpleName());

        eventCount++;
        event.write(scratchBuffer);
    }

    private boolean shouldFlush() {
        return tick - lastFlushTick >= 100 || scratchBuffer.readableBytes() >= 1024 * 1024; // flush every 100 ticks or 1 MiB of data
    }

    private void flush() {
        long dataLength = scratchBuffer.readableBytes();
        long requiredSize = Zstd.compressBound(dataLength);
        if (requiredSize > scratchBuffer.capacity())
            scratchBuffer.resize(requiredSize);

        // Prepare buffers for compression
        segmentBuffer.ensureWritable(requiredSize);
        var targetAddress = networkBufferAddress(segmentBuffer) + segmentBuffer.writeIndex();
        var baseAddress = networkBufferAddress(scratchBuffer);

        // Compress in place
        long compressedLength = Zstd.compressUnsafe(
            targetAddress, requiredSize,
            baseAddress, dataLength,
            ReplayHeader.RECORD_COMPRESSION_LEVEL
        );

        var index = new ChunkIndex(
            lastFlushTick,
            tick - lastFlushTick,
            (byte) 0, // flags
            (((long) segmentIndex) << 32) | (segmentBuffer.writeIndex() & 0xFFFFFFFFL), // TODO: offset
            (int) compressedLength,
            (int) dataLength
        );
        System.out.println("create index " + index);
        this.index.add(index);

        segmentBuffer.advanceWrite(compressedLength);
        scratchBuffer.clear();
        lastFlushTick = tick;

        // TODO: figure out sizes here, definitely bigger than this.
        if (segmentBuffer.writeIndex() > 2048) {
            System.out.println("write segment " + segmentIndex);
            writeSegment(segmentIndex++);
        }
    }

    private void writeSegment(int index) {
        var toWrite = segmentBuffer.read(NetworkBuffer.RAW_BYTES);
        segmentBuffer.clear();

        var metadata = NetworkBuffer.makeArray(buffer -> {
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

        Thread.startVirtualThread(() -> {
            try {
                var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/workspace/replay-test");
                var segmentPath = path.resolve("segment-" + String.format("%04d", index) + ".dat");
                Files.write(segmentPath, toWrite, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                var metadataPath = path.resolve("meta.dat");
                Files.write(metadataPath, metadata, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
