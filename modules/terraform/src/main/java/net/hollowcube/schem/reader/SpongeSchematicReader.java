package net.hollowcube.schem.reader;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.hollowcube.schem.BlockEntityData;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.SpongeSchematic;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.*;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.hollowcube.schem.old.CoordinateUtil.blockIndex;
import static net.hollowcube.schem.reader.ReadHelpers.*;

/**
 * Implements a reader for the Sponge schematic format versions 1-3 (with transparent upgrade).
 *
 * @see <a href="https://github.com/SpongePowered/Schematic-Specification/tree/master">Format Specification</a>
 */
public class SpongeSchematicReader implements SchematicReader {
    private static final int MIN_VERSION = 1;
    private static final int MAX_VERSION = 3;

    private final GameDataProvider gameData = GameDataProvider.provider();

    @Override
    public @NotNull Schematic read(byte @NotNull [] data) throws SchematicReadException {
        try {
            return read(BinaryTagIO.reader().readNamed(
                    new ByteArrayInputStream(data),
                    BinaryTagIO.Compression.GZIP
            ));
        } catch (IOException e) {
            throw new SchematicReadException("failed to read root compound", e);
        }
    }

    public @NotNull Schematic read(@NotNull Map.Entry<String, CompoundBinaryTag> rootPair) {
        var root = rootPair.getValue();
        if ("".equals(rootPair.getKey())) {
            // V3 has the root tag as "", with "Schematic" embedded inside.
            root = getRequired(root, "Schematic", BinaryTagTypes.COMPOUND);
        } else {
            // V1,2 should have the root tag as "Schematic"
            assertTrue("Schematic".equals(rootPair.getKey()), "missing required root tag 'Schematic'");
        }

        int version = root.getInt("Version");
        if (version == 0) version = 1; // Version 0 is not a valid version, but it is used in some old schematics
        assertTrue(version >= MIN_VERSION && version <= MAX_VERSION,
                "unsupported schematic version {0}", version);
        int dataVersion = root.getInt("DataVersion");
        assertTrue(dataVersion >= 0, "invalid data version {0}", dataVersion);
        int dataVersionMax = gameData.dataVersion();

        var metadata = root.getCompound("Metadata"); // Optional, default of empty map is fine

        var width = getRequired(root, "Width", BinaryTagTypes.SHORT).value();
        assertTrue(width > 0, "invalid width {0}", width);
        var height = getRequired(root, "Height", BinaryTagTypes.SHORT).value();
        assertTrue(height > 0, "invalid height {0}", height);
        var length = getRequired(root, "Length", BinaryTagTypes.SHORT).value();
        assertTrue(length > 0, "invalid length {0}", length);
        Point size = new Vec(width, height, length);

        var offset = root.keySet().contains("Offset") ? getRequiredPoint(root, "Offset") : Vec.ZERO;
        if (version < 3 && metadata.keySet().contains("WEOffsetX")) {
            // Offset is the relative offset when creating the schematic, however worldedit sets this to the
            // world position it was created. In reality, we want the position relative to the player position
            // which is set in WEOffsetX|Y|Z for worldedit (and most compatible editors).
            // This was fixed in version 3, where the offset is always relative to the player position and WEOffset is gone.
            var x = getRequired(metadata, "WEOffsetX", BinaryTagTypes.INT);
            var y = getRequired(metadata, "WEOffsetY", BinaryTagTypes.INT);
            var z = getRequired(metadata, "WEOffsetZ", BinaryTagTypes.INT);
            offset = new Vec(x.value(), y.value(), z.value());
        }

        // === Block data ===
        Block[] blockPalette;
        byte[] blockData;
        var blockEntities = new Int2ObjectArrayMap<BlockEntityData>();
        if (version < 3) {
            var blockPaletteMax = root.getInt("PaletteMax", -1);
            // Note(matt): I am aware that Palette is not required, and it should fall back to the global palette
            // if not present. However, this is not currently implemented as there is no way to support upgrading
            // the block ids currently.
            var blockPaletteObject = getRequired(root, "Palette", BinaryTagTypes.COMPOUND);
            blockPalette = new Block[blockPaletteMax < 1 ? blockPaletteObject.size() : blockPaletteMax];
            for (var entry : blockPaletteObject) {
                assertTrue(entry.getValue().type() == BinaryTagTypes.INT, "expected palette entry to be an int");
                var paletteId = ((IntBinaryTag) entry.getValue()).value();
                try {
                    String blockState = entry.getKey();
                    if (dataVersion < dataVersionMax)
                        blockState = gameData.upgradeBlockState(dataVersion, dataVersionMax, blockState);
                    blockPalette[paletteId] = ArgumentBlockState.staticParse(blockState);
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalStateException("invalid block type: " + entry.getKey(), e);
                }
            }
            blockData = getRequired(root, "BlockData", BinaryTagTypes.BYTE_ARRAY).value();

            // === Block entities ===
            var tileEntities = root.getList(version == 1 ? "TileEntities" : "BlockEntities", BinaryTagTypes.COMPOUND);
            for (var tileEntity : tileEntities) {
                var data = (CompoundBinaryTag) tileEntity;
                var id = getRequired(data, "Id", BinaryTagTypes.STRING);
                var pos = getRequiredPoint(data, "Pos");
                // ContentVersion ignored.

                // The rest of the data fields are extracted into a separate data (similar to later versions)
                var extracted = CompoundBinaryTag.builder();
                for (var entry : data) {
                    var key = entry.getKey();
                    if ("Id".equals(key) || "Pos".equals(key) || "ContentVersion".equals(key)) continue;
                    extracted.put(entry.getKey(), entry.getValue());
                }

                var blockEntityData = extracted.build();
                if (dataVersion < dataVersionMax)
                    blockEntityData = gameData.upgradeBlockEntity(dataVersion, dataVersionMax, id.value(), blockEntityData);
                blockEntities.put(blockIndex(size, pos), new BlockEntityData(id.value(), pos, blockEntityData));
            }
        } else {
            var blocksContainer = root.getCompound("Blocks");
            var blockPaletteObject = getRequired(blocksContainer, "Palette", BinaryTagTypes.COMPOUND);
            blockPalette = new Block[blockPaletteObject.size()];
            for (var entry : blockPaletteObject) {
                assertTrue(entry.getValue().type() == BinaryTagTypes.INT, "expected palette entry to be an int");
                var paletteId = ((IntBinaryTag) entry.getValue()).value();
                var block = ArgumentBlockState.staticParse(entry.getKey());

                // Increase the palette size if the input object has missing indices (dumb)
                if (paletteId >= blockPalette.length) {
                    var newPalette = new Block[paletteId + 1];
                    System.arraycopy(blockPalette, 0, newPalette, 0, blockPalette.length);
                    blockPalette = newPalette;
                }

                blockPalette[paletteId] = block;
            }
            for (int i = 0; i < blockPalette.length; i++) {
                if (blockPalette[i] == null) blockPalette[i] = Block.AIR;
            }
            blockData = getRequired(blocksContainer, "Data", BinaryTagTypes.BYTE_ARRAY).value();

            // === Block entities ===
            for (var blockEntityTag : blocksContainer.getList("BlockEntities", BinaryTagTypes.COMPOUND)) {
                var blockEntity = (CompoundBinaryTag) blockEntityTag;
                var id = getRequired(blockEntity, "Id", BinaryTagTypes.STRING);
                var pos = getRequiredPoint(blockEntity, "Pos");
                var data = blockEntity.getCompound("Data");
                if (dataVersion < gameData.dataVersion())
                    data = gameData.upgradeBlockEntity(dataVersion, gameData.dataVersion(), id.value(), data);
                blockEntities.put(blockIndex(size, pos), new BlockEntityData(id.value(), pos, data));
            }
        }

        // === Biome Data ===
        String[] biomePalette = new String[0];
        byte[] biomeData = new byte[0];
        if (version == 2) {
            var biomePaletteMax = root.getInt("BiomePaletteMax", -1);
            var biomePaletteObject = root.getCompound("BiomePalette");
            biomePalette = new String[biomePaletteMax < 1 ? biomePaletteObject.size() : biomePaletteMax];
            for (var entry : biomePaletteObject) {
                var paletteId = ((IntBinaryTag) entry.getValue()).value();
                biomePalette[paletteId] = entry.getKey();
                //todo for version 2 -> 3 i need to inflate the data to 3d.
            }
            biomeData = root.getByteArray("BiomeData");
        } else if (version >= 3) {
            var biomesContainer = root.getCompound("Biomes");
            var biomePaletteObject = biomesContainer.getCompound("Palette");
            biomePalette = new String[biomePaletteObject.size()];
            for (var entry : biomePaletteObject) {
                var paletteId = ((IntBinaryTag) entry.getValue()).value();
                biomePalette[paletteId] = entry.getKey();
            }
            biomeData = biomesContainer.getByteArray("Data");
        }

        // === Entities ===
        var entities = new ArrayList<CompoundBinaryTag>();
        if (version > 1) {
            for (var entityTag : root.getList("Entities", BinaryTagTypes.COMPOUND)) {
                var entity = (CompoundBinaryTag) entityTag;
                if (dataVersion < gameData.dataVersion())
                    entity = gameData.upgradeEntity(dataVersion, gameData.dataVersion(), entity);
                entities.add(entity);
            }
        }

        return new SpongeSchematic(
                metadata, size, offset,
                List.of(blockPalette), ByteArrayBinaryTag.byteArrayBinaryTag(blockData),
                List.of(biomePalette), ByteArrayBinaryTag.byteArrayBinaryTag(biomeData),
                blockEntities, entities
        );
    }

}
