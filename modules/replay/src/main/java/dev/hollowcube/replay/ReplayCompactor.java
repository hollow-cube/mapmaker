package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import net.minestom.server.network.NetworkBuffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static net.minestom.server.network.PolarBufferAccessWidener.networkBufferAddress;

public final class ReplayCompactor {


    // TODO: in the future we may need to deal with not loading the entire thing into memory, for now its ignored.

    static void main() {
        new ReplayCompactor().compact();
    }

    private final Path path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/workspace/replay-test");


    public void compact() {
        try {
            System.out.println("Compacting replay at " + path);

            var outBuffer = NetworkBuffer.resizableBuffer(2048); // TODO: smarter size
            outBuffer.advanceWrite(ReplayHeader.HEADER_LENGTH); // we will patch later

            // READ META FILE

            var metaPath = path.resolve("meta.dat");
            var metaData = Files.readAllBytes(metaPath);
            var metaBuffer = NetworkBuffer.wrap(metaData, 0, metaData.length);

            var header = new ReplayHeader(metaBuffer);

            // copy metadata directly, we dont need to edit it.
            outBuffer.write(NetworkBuffer.RAW_BYTES, metaBuffer.read(NetworkBuffer.FixedRawBytes(header.metadataLength())));

            // skip by index length in output buffer, we will patch this later.
            outBuffer.advanceWrite(header.indexLength());

            var index = new ArrayList<ChunkIndex>(header.chunkCount());
            for (int i = 0; i < header.chunkCount(); i++) {
                index.add(metaBuffer.read(ChunkIndex.NETWORK_TYPE));
            }

            // Stats tracking
            long totalUncompressed = 0;
            long totalOriginalCompressed = 0;
            long totalNewCompressed = 0;

            for (int i = 0; i < header.chunkCount(); i++) {
                var chunk = index.get(i);
                var segmentIndex = (int) (chunk.byteOffset() >> 32);
                var segmentOffset = chunk.byteOffset() & 0xFFFFFFFFL;
                var segmentBuffer = readSegment(segmentIndex);

                // The recompression here is:
                // - ensure writeBuffer has enough space for in-place compression
                // - decompress into the end of write buffer
                // - recompress in-place in write buffer

                long requiredSize = Zstd.compressBound(chunk.uncompressedLength());
                outBuffer.ensureWritable(requiredSize);

                long byteOffset = outBuffer.writeIndex();
                long decompressAddress = networkBufferAddress(outBuffer) + byteOffset + (requiredSize - chunk.uncompressedLength());
                Zstd.decompressUnsafe(
                    decompressAddress,
                    chunk.uncompressedLength(),
                    networkBufferAddress(segmentBuffer) + segmentOffset,
                    chunk.compressedLength()
                );
                long newCompressedLength = Zstd.compressUnsafe(
                    networkBufferAddress(outBuffer) + byteOffset,
                    requiredSize,
                    decompressAddress,
                    chunk.uncompressedLength(),
                    ReplayHeader.COMPACT_COMPRESSION_LEVEL
                );

                index.set(i, chunk.withCompaction(byteOffset, (int) newCompressedLength));
                outBuffer.advanceWrite(newCompressedLength);

                // Per-chunk stats
                int uncompressed = chunk.uncompressedLength();
                int originalCompressed = chunk.compressedLength();
                double originalRatio = (double) uncompressed / originalCompressed;
                double newRatio = (double) uncompressed / newCompressedLength;
                double improvement = (1.0 - (double) newCompressedLength / originalCompressed) * 100.0;

                System.out.printf(
                    "chunk %d: %d -> %d -> %d bytes (orig %.2fx, new %.2fx, %+.1f%%)%n",
                    i, uncompressed, originalCompressed, newCompressedLength,
                    originalRatio, newRatio, -improvement
                );

                totalUncompressed += uncompressed;
                totalOriginalCompressed += originalCompressed;
                totalNewCompressed += newCompressedLength;

                // todo should reuse the same zstd context here and for the initial write as a small optimization
            }

            long outSize = outBuffer.writeIndex();

            // Patch the header
            outBuffer.writeIndex(0);
            header.write(outBuffer);

            // Patch the index
            outBuffer.writeIndex(header.indexByteOffset());
            for (var chunk : index) {
                outBuffer.write(ChunkIndex.NETWORK_TYPE, chunk);
            }

            outBuffer.writeIndex(outSize);
            var bytes = outBuffer.read(NetworkBuffer.RAW_BYTES);
            Files.write(path.resolve("test1-compact.dat"), bytes);

            // Summary
            double avgOriginalRatio = (double) totalUncompressed / totalOriginalCompressed;
            double avgNewRatio = (double) totalUncompressed / totalNewCompressed;
            double overallReduction = (1.0 - (double) totalNewCompressed / totalOriginalCompressed) * 100.0;
            long fileSize = bytes.length;

            System.out.println();
            System.out.println("=== Compaction Summary ===");
            System.out.printf("Chunks:              %d%n", header.chunkCount());
            System.out.printf("Uncompressed total:  %d bytes (%.2f KiB)%n", totalUncompressed, totalUncompressed / 1024.0);
            System.out.printf("Original compressed: %d bytes (%.2f KiB)  ratio %.2fx%n",
                totalOriginalCompressed, totalOriginalCompressed / 1024.0, avgOriginalRatio);
            System.out.printf("New compressed:      %d bytes (%.2f KiB)  ratio %.2fx%n",
                totalNewCompressed, totalNewCompressed / 1024.0, avgNewRatio);
            System.out.printf("Reduction:           %.1f%% smaller%n", overallReduction);
            System.out.printf("Final file size:     %d bytes (%.2f KiB)%n", fileSize, fileSize / 1024.0);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private NetworkBuffer readSegment(int index) throws IOException {
        var segmentPath = path.resolve("segment-" + String.format("%04d", index) + ".dat");
        var segmentData = Files.readAllBytes(segmentPath);
        return NetworkBuffer.wrap(segmentData, 0, segmentData.length);
        // todo cache me
    }


}