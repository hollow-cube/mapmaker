package net.hollowcube.schem.reader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.hollowcube.schem.BlockEntityData;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.SpongeSchematic;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static net.hollowcube.schem.old.CoordinateUtil.blockIndex;
import static net.hollowcube.schem.reader.ReadHelpers.*;

// MCEdit/Schematica/WE pre 1.13
@SuppressWarnings("UnstableApiUsage")
public class MCEditSchematicReader implements SchematicReader {
    private static final Logger logger = LoggerFactory.getLogger(MCEditSchematicReader.class);
    private static final Map<String, Block> LEGACY_BLOCKS;

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

    public @NotNull Schematic read(@NotNull Map.Entry<String, CompoundBinaryTag> rootPair) {
        assertTrue("Schematic".equals(rootPair.getKey()), "missing required root tag 'Schematic'");
        var root = rootPair.getValue();

        var width = getRequired(root, "Width", BinaryTagTypes.SHORT).value();
        assertTrue(width > 0, "invalid width {0}", width);
        var height = getRequired(root, "Height", BinaryTagTypes.SHORT).value();
        assertTrue(height > 0, "invalid height {0}", height);
        var length = getRequired(root, "Length", BinaryTagTypes.SHORT).value();
        assertTrue(length > 0, "invalid length {0}", length);
        var size = new Vec(width, height, length);
        var maxIndices = width * height * length;

        var offsetX = root.getInt("WEOffsetX");
        var offsetY = root.getInt("WEOffsetY");
        var offsetZ = root.getInt("WEOffsetZ");
        var offset = new Vec(offsetX, offsetY, offsetZ);

        // === Blocks ===
        var blockIds = root.getByteArray("Blocks");
        var blockDataBits = root.getByteArray("Data");
        var updatedPalette = new ArrayList<Block>();
        var updatedBlockData = new int[maxIndices];
        for (int i = 0; i < maxIndices; i++) {
            int blockId = blockIds[i] & 255;
            int blockData = blockDataBits[i] & 255;

            var newBlock = LEGACY_BLOCKS.get(String.format("%d:%d", blockId, blockData));
            if (newBlock == null) throw new SchematicReadException("failed to find block for legacy id " + blockId);

            int paletteIndex = updatedPalette.indexOf(newBlock);
            if (paletteIndex == -1) {
                paletteIndex = updatedPalette.size();
                updatedPalette.add(newBlock);
            }
            updatedBlockData[i] = paletteIndex;
        }
        var blockData = NetworkBuffer.makeArray(buffer -> {
            for (var data : updatedBlockData)
                buffer.write(NetworkBuffer.VAR_INT, data);
        });

        // === Block Entities ===
        var blockEntities = new Int2ObjectArrayMap<BlockEntityData>();
        for (var blockEntityTag : root.getList("TileEntities", BinaryTagTypes.COMPOUND)) {
            var base = (CompoundBinaryTag) blockEntityTag;
            var id = getRequired(base, "id", BinaryTagTypes.STRING).value();
            var pos = getRequiredVec3(base, "");
            var data = base.remove("id").remove("x").remove("y").remove("z");
            blockEntities.put(blockIndex(size, pos), new BlockEntityData(id, pos,
                    // Always try to upgrade since this is always a legacy format
                    gameData.upgradeBlockEntity(GameDataProvider.DATA_VERSION_UNKNOWN, gameData.dataVersion(), id, data)));
        }

        // === Entities ===
        var entities = new ArrayList<CompoundBinaryTag>();
        for (var entityTag : root.getList("Entities", BinaryTagTypes.COMPOUND)) {
            // Always try to upgrade since this is always a legacy format
            entities.add(gameData.upgradeEntity(GameDataProvider.DATA_VERSION_UNKNOWN, gameData.dataVersion(), (CompoundBinaryTag) entityTag));
        }

        return new SpongeSchematic(
                CompoundBinaryTag.empty(), size, offset,
                updatedPalette, ByteArrayBinaryTag.byteArrayBinaryTag(blockData),
                List.of(), SpongeSchematic.EMPTY_BYTE_ARRAY,
                blockEntities, entities
        );
    }

    static {
        var entries = new HashMap<String, Block>();
        try (var is = MCEditSchematicReader.class.getResourceAsStream("/net/hollowcube/schem/legacy_blocks.json")) {
            Objects.requireNonNull(is, "legacy_blocks.json not found");
            var blockData = new Gson().fromJson(new InputStreamReader(is), JsonObject.class);
            for (var entry : blockData.entrySet())
                entries.put(entry.getKey(), ArgumentBlockState.staticParse(entry.getValue().getAsString()));
        } catch (Exception e) {
            logger.error("failed to load legacy blocks (loading a legacy schematic will fail): {}", e.getMessage());
        }
        LEGACY_BLOCKS = Map.copyOf(entries);
    }
}
