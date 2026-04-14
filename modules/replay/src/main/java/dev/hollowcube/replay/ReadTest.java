package dev.hollowcube.replay;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.network.NetworkBuffer;

import java.nio.file.Files;
import java.nio.file.Path;

import static net.minestom.server.network.PolarBufferAccessWidener.networkBufferAddress;

public class ReadTest {
    static void main() throws Exception {
        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/workspace/replay-test/test1-compact.dat");
        var bytes = Files.readAllBytes(path);
        var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);

        System.out.println("file: " + path.getFileName().toString() + " (" + bytes.length + " bytes)");

        var header = new ReplayHeader(buffer);
        System.out.println("version: " + header.version());
        System.out.println("worldId: " + header.worldId());
        System.out.println("worldVersion: " + header.worldVersion());
        System.out.println("timestamp: " + header.timestamp());
        System.out.println("dictionary: " + header.dictionary());
        System.out.println("metadataLength: " + header.metadataLength());
        System.out.println("indexLength: " + header.indexLength());
        System.out.println("tickCount: " + header.tickCount());
        System.out.println("chunkCount: " + header.chunkCount());

        var metadata = buffer.read(NetworkBuffer.NBT_COMPOUND);
        System.out.println("\nmetadata: " + MinestomAdventure.tagStringIO().asString(metadata));

        var index = new ChunkIndex[header.chunkCount()];
        for (int i = 0; i < header.chunkCount(); i++) {
            index[i] = buffer.read(ChunkIndex.NETWORK_TYPE);
        }

        var scratch = NetworkBuffer.resizableBuffer(2048);

        for (int i = 0; i < header.chunkCount(); i++) {
            var chunk = index[i];
            System.out.println("\nchunk: " + chunk);

            scratch.clear();
            scratch.ensureWritable(chunk.uncompressedLength());
            Zstd.decompressUnsafe(
                networkBufferAddress(scratch), chunk.uncompressedLength(),
                networkBufferAddress(buffer) + chunk.byteOffset(), chunk.compressedLength()
            );

            for (int tick = chunk.startTick(); tick < chunk.startTick() + chunk.tickCount(); tick++) {
                var tickIndex = scratch.read(NetworkBuffer.VAR_INT);
                var eventCount = scratch.read(NetworkBuffer.SHORT);
                System.out.println(tick + " (" + tickIndex + "): " + eventCount + " events");

                for (int j = 0; j < eventCount; j++) {
                    var typeId = scratch.read(NetworkBuffer.VAR_INT);
                    var entityId = scratch.read(NetworkBuffer.VAR_INT);
                    var delta = scratch.read(NetworkBuffer.POS);
                    System.out.println("  " + typeId + " " + entityId + " " + delta);
                }

            }


        }

    }
}
