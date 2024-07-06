package net.hollowcube.schem.reader;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.schem.BlockEntityData;
import net.hollowcube.schem.LitematicaSchematic;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.SpongeSchematic;
import net.hollowcube.schem.old.CoordinateUtil;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.hollowcube.schem.old.CoordinateUtil.blockIndex;
import static net.hollowcube.schem.reader.ReadHelpers.*;

@SuppressWarnings("UnstableApiUsage")
public class LitematicaSchematicReader implements SchematicReader {
    private final GameDataProvider gameData = GameDataProvider.provider();

    @Override
    public @NotNull Schematic read(byte @NotNull [] data) throws IOException {
        try {
            return read(BinaryTagIO.reader().readNamed(
                    new ByteArrayInputStream(data),
                    BinaryTagIO.Compression.GZIP
            ));
        } catch (IOException e) {
            throw new SchematicReadException("failed to read root compound", e);
        }
    }

    @ApiStatus.Internal
    public @NotNull Schematic read(@NotNull Map.Entry<String, CompoundBinaryTag> rootPair) {
        assertTrue("".equals(rootPair.getKey()), "root tag must be empty, was: '{0}'", rootPair.getKey());
        var root = rootPair.getValue();
        var dataVersion = getRequired(root, "MinecraftDataVersion", BinaryTagTypes.INT).value();
        var version = getRequired(root, "Version", BinaryTagTypes.INT).value();
        var subVersion = getRequired(root, "SubVersion", BinaryTagTypes.INT).value();
        assertTrue(version == 6 && subVersion == 1, "unsupported version (only 6.1 is supported): {0}.{1}", version, subVersion);

        var metadata = getRequired(root, "Metadata", BinaryTagTypes.COMPOUND);
        var enclosingSize = getRequiredVec3(metadata, "EnclosingSize");
        assertTrue(enclosingSize.blockX() > 0, "invalid enclosing width: {0}", enclosingSize.blockX());
        assertTrue(enclosingSize.blockY() > 0, "invalid enclosing height: {0}", enclosingSize.blockY());
        assertTrue(enclosingSize.blockZ() > 0, "invalid enclosing length: {0}", enclosingSize.blockZ());

        var regions = new HashMap<String, Schematic>();
        for (var regionPair : getRequired(root, "Regions", BinaryTagTypes.COMPOUND)) {
            var region = loadRegion(dataVersion, (CompoundBinaryTag) regionPair.getValue());
            regions.put(regionPair.getKey(), region);
        }

        return new LitematicaSchematic(metadata, enclosingSize, regions);
    }

    private @NotNull Schematic loadRegion(int dataVersion, @NotNull CompoundBinaryTag region) {
        var rawPos = getRequiredVec3(region, "Position");
        var rawSize = getRequiredVec3(region, "Size")
                .withX(x -> x >= 0 ? x - 1 : x + 1)
                .withY(y -> y >= 0 ? y - 1 : y + 1)
                .withZ(z -> z >= 0 ? z - 1 : z + 1);
        var relativeEnd = rawSize.add(rawPos);
        var absoluteMin = CoordinateUtil.min(rawPos, relativeEnd);
        var absoluteMax = CoordinateUtil.max(rawPos, relativeEnd);
        var size = absoluteMax.sub(absoluteMin).add(1);

        // === Block Palette ===
        var paletteList = getRequired(region, "BlockStatePalette", BinaryTagTypes.LIST);
        var blockPalette = new Block[paletteList.size()];
        for (int i = 0; i < paletteList.size(); i++)
            blockPalette[i] = readBlockState(paletteList.getCompound(i));

        // Litematica doesn't include the block entity id in TileEntities, so we have to look up
        // the block IDs from the palette while creating blockData to get the block id.
        // We can use the Minestom registry to get the associated block entity and store that too.
        Int2ObjectMap<CompoundBinaryTag> blockEntityData = new Int2ObjectOpenHashMap<>();
        for (var blockEntityTag : region.getList("TileEntities", BinaryTagTypes.COMPOUND)) {
            var blockEntity = (CompoundBinaryTag) blockEntityTag;
            var pos = getRequiredVec3(blockEntity, "");
            var data = blockEntity.remove("x").remove("y").remove("z");

            var index = (int) (pos.blockX() + pos.blockZ() * size.x() + pos.blockY() * size.x() * size.z());
            blockEntityData.put(index, data);
        }

        var blockEntities = new Int2ObjectArrayMap<BlockEntityData>();
        var packedBlocks = getRequired(region, "BlockStates", BinaryTagTypes.LONG_ARRAY).value();
        var unpackedBlocks = new int[size.blockX() * size.blockY() * size.blockZ()];
        var bitsPerEntry = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(blockPalette.length - 1));
        ReadHelpers.unpackPaletteTight(unpackedBlocks, packedBlocks, bitsPerEntry);
        var blockData = ByteArrayBinaryTag.byteArrayBinaryTag(NetworkBuffer.makeArray(buffer -> {
            for (int index = 0; index < unpackedBlocks.length; index++) {
                var paletteIndex = unpackedBlocks[index];
                buffer.write(NetworkBuffer.VAR_INT, paletteIndex);

                // Try to find block entity for this block
                var blockEntityId = blockPalette[paletteIndex].registry().blockEntity();
                if (blockEntityId != null) {
                    var blockEntity = blockEntityData.getOrDefault(index, CompoundBinaryTag.empty());
                    var blockPosition = new Vec(
                            index % size.x(),
                            (index / size.x()) % size.z(),
                            index / (size.x() * size.z())
                    );
                    if (dataVersion != gameData.dataVersion()) {
                        blockEntity = gameData.upgradeBlockEntity(dataVersion, gameData.dataVersion(), blockEntityId, blockEntity);
                    }
                    blockEntities.put(blockIndex(size, blockPosition), new BlockEntityData(blockEntityId, blockPosition, blockEntity));
                }
            }
        }));

        // === Entities ===
        var entities = new ArrayList<CompoundBinaryTag>();
        for (var entityTag : region.getList("Entities", BinaryTagTypes.COMPOUND)) {
            var entity = (CompoundBinaryTag) entityTag;
            if (dataVersion != gameData.dataVersion())
                entity = gameData.upgradeEntity(dataVersion, gameData.dataVersion(), entity);
            entities.add(entity);
        }

        return new SpongeSchematic(
                CompoundBinaryTag.empty(), size, absoluteMin,
                List.of(blockPalette), blockData,
                List.of(), SpongeSchematic.EMPTY_BYTE_ARRAY,
                blockEntities, entities
        );
    }
}
