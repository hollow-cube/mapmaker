package net.hollowcube.schem.reader;

import net.hollowcube.schem.BlockEntityData;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.Structure;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static net.hollowcube.schem.reader.ReadHelpers.*;

public class StructureReader implements SchematicReader {

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
        assertTrue("".equals(rootPair.getKey()), "root tag must be empty, was: '{0}'", rootPair.getKey());
        var root = rootPair.getValue();
        var dataVersion = getRequired(root, "DataVersion", BinaryTagTypes.INT);

        var size = getRequiredPoint(root, "size");

        // === Palettes ===
        int paletteSize = -1;
        var palettes = new ArrayList<Block[]>();
        var singlePalette = root.getList("palette", BinaryTagTypes.COMPOUND);
        if (singlePalette.size() != 0) {
            var palette = new Block[singlePalette.size()];
            for (int i = 0; i < singlePalette.size(); i++)
                palette[i++] = readBlockState(singlePalette.getCompound(i));
            palettes.add(palette);
            paletteSize = palette.length;
        } else {
            var multiPalette = root.getList("palettes", BinaryTagTypes.LIST);
            for (var innerPaletteRaw : multiPalette) {
                var innerPalette = (ListBinaryTag) innerPaletteRaw;
                var palette = new Block[innerPalette.size()];
                for (int i = 0; i < innerPalette.size(); i++)
                    palette[i++] = readBlockState(innerPalette.getCompound(i));
                palettes.add(palette);

                if (paletteSize == -1) paletteSize = palette.length;
                else assertTrue(paletteSize == palette.length, "palette sizes must be consistent");
            }
        }
        assertTrue(paletteSize > 0, "palette(s) must be provided");

        // === Blocks ===
        var blocks = new ArrayList<Structure.BlockInfo>();
        for (var block : root.getList("blocks", BinaryTagTypes.COMPOUND)) {
            var blockCompound = (CompoundBinaryTag) block;
            var pos = getRequiredPoint(blockCompound, "pos");
            var state = getRequired(blockCompound, "state", BinaryTagTypes.INT).value();
            assertTrue(state >= 0 && state < paletteSize, "invalid palette index {0}", state);

            BlockEntityData blockEntity = null;
            if (blockCompound.get("nbt") instanceof CompoundBinaryTag nbt) {
                var id = getRequired(nbt, "id", BinaryTagTypes.STRING).value();
                blockEntity = new BlockEntityData(id, pos, nbt.remove("id"));
            }

            blocks.add(new Structure.BlockInfo(pos, state, blockEntity));
        }

        // === Entities ===
        var entities = new ArrayList<CompoundBinaryTag>();
        for (var entity : root.getList("Entities", BinaryTagTypes.COMPOUND)) {
            entities.add((CompoundBinaryTag) entity);
        }

        return new Structure(size, blocks, palettes, entities);
    }
}
