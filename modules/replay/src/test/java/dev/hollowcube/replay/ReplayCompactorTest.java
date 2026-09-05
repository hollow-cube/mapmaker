package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.io.CompactedReplayReader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// Compaction is driven directly here, rather than through a recording, so that what it does with a
/// chunk stays testable without depending on how events happen to encode.
final class ReplayCompactorTest {

    @Test
    void compactionSurvivesAChunkThatNarrowsPastAVarIntBoundary() {
        // Recompression only ever gets to keep the index the same length by luck. This payload
        // takes 129 bytes at the recording level and 79 at the compaction level, so its length
        // stops needing two varint bytes and the compacted index is a byte shorter than the
        // segmented one it came from.
        var payload = structuredPayload(46);
        var compressed = Zstd.compress(payload, ReplayHeader.RECORD_COMPRESSION_LEVEL);
        assertEquals(2, varIntLength(compressed.length), "payload no longer straddles the boundary");

        var preamble = segmentedPreamble(compressed.length, payload.length);
        var result = ReplayCompactor.compact(preamble, segmentIndex -> {
            assertEquals(0, segmentIndex);
            return compressed;
        });

        try (var reader = new CompactedReplayReader(result.data())) {
            // The preamble storage keeps has to be exactly the prefix that describes the rest, so
            // the header's own lengths have to add up to it.
            var compactedHeader = reader.header();
            assertEquals(
                result.preambleLength(),
                ReplayHeader.HEADER_LENGTH + compactedHeader.metadataLength() + compactedHeader.indexLength()
            );

            var chunk = reader.index().getFirst();
            assertEquals(1, varIntLength(chunk.compressedLength()), "the chunk should have narrowed");
            assertEquals(result.preambleLength(), chunk.byteOffset());
            assertEquals(result.data().length, chunk.byteOffset() + chunk.compressedLength());

            var read = reader.chunk(chunk);
            assertNotNull(read);
            assertArrayEquals(payload, read.read(NetworkBuffer.RAW_BYTES));
        }
    }

    @Test
    void oversizedItemChunksDoNotResolveItemsAgainstTheWorkersRegistries() {
        var item = CompoundBinaryTag.builder().putString("id", "old:unknown_item")
            .putString("payload", "x".repeat(4096)).build();
        var payload = NetworkBuffer.makeArray(buffer -> {
            for (var tick = 0; tick < 100; tick++) {
                buffer.write(NetworkBuffer.VAR_INT, tick);
                buffer.write(NetworkBuffer.SHORT, (short) 1);
                buffer.write(NetworkBuffer.VAR_INT, 5); // SetItemEvent
                buffer.write(NetworkBuffer.VAR_INT, 0); // entity
                buffer.write(NetworkBuffer.VAR_INT, 1); // slots
                buffer.write(NetworkBuffer.VAR_INT, 0);
                buffer.write(NetworkBuffer.NBT_COMPOUND, item);
            }
        });
        var compressed = Zstd.compress(payload);
        var result = ReplayCompactor.compact(segmentedPreamble(compressed.length, payload.length, 100),
            ignored -> compressed);
        try (var reader = new CompactedReplayReader(result.data())) {
            var decoded = NetworkBuffer.resizableBuffer();
            for (var frame : reader.index()) {
                assertTrue(frame.uncompressedLength() <= ReplayCompactor.FRAME_BYTE_LIMIT);
                decoded.write(NetworkBuffer.RAW_BYTES, reader.chunk(frame).read(NetworkBuffer.RAW_BYTES));
            }
            assertArrayEquals(payload, decoded.read(NetworkBuffer.RAW_BYTES));
        }
    }

    /// Records of a small counter and a mostly-constant blob, which is roughly the shape of a tick
    /// stream and compresses the way one does.
    private static byte[] structuredPayload(int records) {
        var payload = new byte[records * 14];
        for (var record = 0; record < records; record++) {
            payload[record * 14] = (byte) (record & 0x7F);
            payload[record * 14 + 1] = (byte) ((record >> 7) & 0x7F);
            payload[record * 14 + 2] = 1;
            payload[record * 14 + 4] = 42;
        }
        return payload;
    }

    private static ReplayPreamble segmentedPreamble(int compressedLength, int uncompressedLength) {
        return segmentedPreamble(compressedLength, uncompressedLength, 1);
    }

    private static ReplayPreamble segmentedPreamble(int compressedLength, int uncompressedLength, int ticks) {
        var header = new ReplayHeader(UUID.randomUUID(), ReplayHeader.worldVersion(UUID.randomUUID()));
        var index = List.of(new ChunkIndex(
            0, ticks, ChunkIndex.FLAG_HAS_SNAPSHOT, 0, compressedLength, uncompressedLength));

        var metadata = NetworkBuffer.makeArray(NetworkBuffer.NBT_COMPOUND, CompoundBinaryTag.empty());
        var indexLength = NetworkBuffer.makeArray(buffer -> {
            for (var chunk : index) buffer.write(ChunkIndex.NETWORK_TYPE, chunk);
        }).length;
        header.update(metadata.length, indexLength, ticks, index.size());

        return new ReplayPreamble(header, CompoundBinaryTag.empty(), index);
    }

    private static int varIntLength(int value) {
        var length = 1;
        while (value >= 128) {
            value >>= 7;
            length++;
        }
        return length;
    }
}
