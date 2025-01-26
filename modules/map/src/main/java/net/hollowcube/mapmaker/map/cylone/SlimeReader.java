package net.hollowcube.mapmaker.map.cylone;

import com.github.luben.zstd.Zstd;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlimeReader {
    private static final int SECTION_COUNT = 24;

    private static final NetworkBuffer.Type<byte[]> LIGHT_ARRAY = NetworkBuffer.FixedRawBytes(2048);

    public static SlimeWorld read(byte[] worldData) throws Exception {
        var buffer = NetworkBuffer.wrap(worldData, 0, worldData.length);

        byte[] header = buffer.read(NetworkBuffer.FixedRawBytes(2));
        if (header[0] != -79 || header[1] != 11) {
            throw new IllegalArgumentException("Invalid header");
        }
        int version = buffer.read(NetworkBuffer.BYTE);
        if (version < 10 || version > 12)
            throw new UnsupportedOperationException("Unsupported slime version: " + version);
        int worldVersion = buffer.read(NetworkBuffer.INT);

        var blockEntities = new ArrayList<CompoundBinaryTag>();
        var entities = new ArrayList<CompoundBinaryTag>();
        var chunkBytes = readCompressed(buffer);
        var chunks = readChunks(version, chunkBytes, blockEntities, entities);

        if (version <= 10) {
            var tileEntitiesRaw = readCompound(readCompressed(buffer));
            for (var compound : tileEntitiesRaw.getList("tiles", BinaryTagTypes.COMPOUND)) {
                blockEntities.add((CompoundBinaryTag) compound);
            }

            var entitiesCompound = readCompound(readCompressed(buffer));
            for (var compound : entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND)) {
                entities.add((CompoundBinaryTag) compound);
            }
        }
        var extra = readCompound(readCompressed(buffer));

//        var cdata = extra.getByteArray("cdata_-32_-16");
//        var t = BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(cdata));
//        var a = BinaryTagIO.writer().write(t, new ByteArrayOutputStream());
        // Double.longBitsToDOuble or whatever

        return new SlimeWorld(worldVersion, chunks, blockEntities, entities);
    }

    record SlimeWorld(int dataVersion, Map<ChunkPos, SlimeChunk> chunks, List<CompoundBinaryTag> blockEntities,
                      List<CompoundBinaryTag> entities) {
    }

    record ChunkPos(int x, int z) {
    }

    record SlimeSection(CompoundBinaryTag blockStateTag, CompoundBinaryTag biomeTag, byte[] blockLightArray,
                        byte[] skyLightArray) {
    }

    record SlimeChunk(int x, int z, SlimeSection[] sections, CompoundBinaryTag heightMaps) {
    }

    private static Map<ChunkPos, SlimeChunk> readChunks(int slimeVersion, byte[] data, @NotNull List<CompoundBinaryTag> blockEntities, @NotNull List<CompoundBinaryTag> entities) {
        var buffer = NetworkBuffer.wrap(data, 0, data.length);
        Map<ChunkPos, SlimeChunk> chunkMap = new HashMap<>();

        int chunks = buffer.read(NetworkBuffer.INT);
        for (int i = 0; i < chunks; i++) {
            int x = buffer.read(NetworkBuffer.INT);
            int z = buffer.read(NetworkBuffer.INT);

            CompoundBinaryTag heightmaps = CompoundBinaryTag.empty();
            if (slimeVersion <= 10) {
                var heightMapDataLen = buffer.read(NetworkBuffer.INT);
                var heightMapData = buffer.read(NetworkBuffer.FixedRawBytes(heightMapDataLen));
                heightmaps = readCompound(heightMapData);
            }

            SlimeSection[] sectionArray = new SlimeSection[SECTION_COUNT];
            int sectionCount = buffer.read(NetworkBuffer.INT);
            for (int sectionId = 0; sectionId < sectionCount; sectionId++) {
                byte[] blockLightArray = null;
                if (buffer.read(NetworkBuffer.BOOLEAN)) {
                    blockLightArray = buffer.read(LIGHT_ARRAY);
                }
                byte[] skyLightArray = null;
                if (buffer.read(NetworkBuffer.BOOLEAN)) {
                    skyLightArray = buffer.read(LIGHT_ARRAY);
                }

                var blockStateDataLen = buffer.read(NetworkBuffer.INT);
                byte[] blockStateData = buffer.read(NetworkBuffer.FixedRawBytes(blockStateDataLen));
                var blockStateTag = readCompound(blockStateData);

                var biomeStateDataLen = buffer.read(NetworkBuffer.INT);
                byte[] biomeStateData = buffer.read(NetworkBuffer.FixedRawBytes(biomeStateDataLen));
                var biomeTag = readCompound(biomeStateData);

                sectionArray[sectionId] = new SlimeSection(blockStateTag, biomeTag, blockLightArray, skyLightArray);
            }

            if (slimeVersion > 10) {
                var heightMapDataLen = buffer.read(NetworkBuffer.INT);
                var heightMapData = buffer.read(NetworkBuffer.FixedRawBytes(heightMapDataLen));
                heightmaps = readCompound(heightMapData);

                var blockEntitiesCompound = readCompound(buffer);
                for (var compound : blockEntitiesCompound.getList("tileEntities", BinaryTagTypes.COMPOUND)) {
                    blockEntities.add((CompoundBinaryTag) compound);
                }

                var entitiesCompound = readCompound(buffer);
                for (var compound : entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND)) {
                    entities.add((CompoundBinaryTag) compound);
                }
            }

            chunkMap.put(new ChunkPos(x, z), new SlimeChunk(x, z, sectionArray, heightmaps));
        }

        return chunkMap;
    }

    private static byte[] readCompressed(NetworkBuffer buffer) {
        int compressedLength = buffer.read(NetworkBuffer.INT);
        int normalLength = buffer.read(NetworkBuffer.INT);

        byte[] compressed = buffer.read(NetworkBuffer.FixedRawBytes(compressedLength));
        byte[] normal = new byte[normalLength];

        Zstd.decompress(normal, compressed);
        return normal;
    }

    private static CompoundBinaryTag readCompound(NetworkBuffer buffer) {
        int length = buffer.read(NetworkBuffer.INT);
        return readCompound(buffer.read(NetworkBuffer.FixedRawBytes(length)));
    }

    private static CompoundBinaryTag readCompound(byte[] input) {
        if (input.length == 0) return CompoundBinaryTag.empty();

        try {
            return BinaryTagIO.unlimitedReader().read(
                    new ByteArrayInputStream(input),
                    BinaryTagIO.Compression.NONE
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
