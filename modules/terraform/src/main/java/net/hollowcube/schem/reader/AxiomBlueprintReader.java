package net.hollowcube.schem.reader;

import net.hollowcube.schem.AxiomBlueprint;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.*;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.schem.reader.ReadHelpers.*;

public class AxiomBlueprintReader implements SchematicReader {
    private final GameDataProvider gameData = GameDataProvider.provider();

    @Override
    public @NotNull Schematic read(byte @NotNull [] data) throws IOException {
        var dis = new DataInputStream(new ByteArrayInputStream(data));
        assertTrue(dis.readInt() == AxiomBlueprint.MAGIC_NUMBER, "invalid magic number");

        var rawHeader = dis.readNBytes(dis.readInt());
        var header = BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(rawHeader));
        var thumbnail = ByteArrayBinaryTag.byteArrayBinaryTag(dis.readNBytes(dis.readInt()));

        var rawBlockData = dis.readNBytes(dis.readInt());
        var blockDataNbt = BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(rawBlockData), BinaryTagIO.Compression.GZIP);
        var dataVersion = blockDataNbt.getInt("DataVersion", gameData.dataVersion());

        var regions = new ArrayList<AxiomBlueprint.SectionData>();
        var regionList = blockDataNbt.getList("BlockRegion", BinaryTagTypes.COMPOUND);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (var region : regionList) {
            var blockRegion = (CompoundBinaryTag) region;
            var cx = getRequired(blockRegion, "X", BinaryTagTypes.INT).value();
            var cy = getRequired(blockRegion, "Y", BinaryTagTypes.INT).value();
            var cz = getRequired(blockRegion, "Z", BinaryTagTypes.INT).value();
            var chunkPos = new Vec(cx, cy, cz);
            minX = Math.min(minX, cx);
            minY = Math.min(minY, cy);
            minZ = Math.min(minZ, cz);
            maxX = Math.max(maxX, cx);
            maxY = Math.max(maxY, cy);
            maxZ = Math.max(maxZ, cz);

            var blockStates = getRequired(blockRegion, "BlockStates", BinaryTagTypes.COMPOUND);

            var blockPalette = getRequired(blockStates, "palette", BinaryTagTypes.LIST);
            var palette = new Block[blockPalette.size()];
            for (int i = 0; i < blockPalette.size(); i++) {
                palette[i] = readBlockState(blockPalette.getCompound(i));
            }

            IntArrayBinaryTag blockData = null;
            if (blockStates.keySet().contains("data")) {
                var packedBlocks = getRequired(blockStates, "data", BinaryTagTypes.LONG_ARRAY).value();
                var unpackedBlocks = new int[AxiomBlueprint.BLOCK_PALETTE_SIZE];
                var bitsPerEntry = packedBlocks.length * 64 / AxiomBlueprint.BLOCK_PALETTE_SIZE;
                ReadHelpers.unpackPalette(unpackedBlocks, packedBlocks, bitsPerEntry);
                blockData = IntArrayBinaryTag.intArrayBinaryTag(unpackedBlocks);
            }

            regions.add(new AxiomBlueprint.SectionData(chunkPos, List.of(palette), blockData));

            //todo block entity support
        }

        var size = new Vec((maxX - minX + 1) * 16, (maxY - minY + 1) * 16, (maxZ - minZ + 1) * 16);

        return new AxiomBlueprint(header, thumbnail, size, List.of(), regions);
    }
}
