package net.hollowcube.schem.writer;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.Structure;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static net.hollowcube.schem.writer.WriteHelpers.assertTrue;
import static net.hollowcube.schem.writer.WriteHelpers.writeBlockState;

public class StructureWriter implements SchematicWriter {
    private final GameDataProvider gameData = GameDataProvider.provider();

    @Override
    public byte @NotNull [] write(@NotNull Schematic schematic) {
        try {
            var out = new ByteArrayOutputStream();
            BinaryTagIO.writer().writeNamed(Map.entry("", createRoot(schematic)), out, BinaryTagIO.Compression.GZIP);
            return out.toByteArray();
        } catch (IOException e) {
            throw new SchematicWriteException("failed to write schematic", e);
        }
    }

    private @NotNull CompoundBinaryTag createRoot(@NotNull Schematic anySchematic) {
        var schematic = fromGenericSchematic(anySchematic);
        var root = CompoundBinaryTag.builder();
        root.putInt("DataVersion", gameData.dataVersion());

        var size = schematic.size();
        root.putIntArray("size", new int[]{size.blockX(), size.blockY(), size.blockZ()});

        // Palettes
        var palettes = schematic.palettes();
        if (palettes.size() == 1) {
            root.put("palette", writeBlockPalette(palettes.get(0)));
        } else {
            var paletteList = ListBinaryTag.builder(BinaryTagTypes.LIST);
            for (var palette : palettes) {
                paletteList.add(writeBlockPalette(palette));
            }
            root.put("palettes", paletteList.build());
        }

        // Blocks
        var blocks = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
        for (var block : schematic.blocks()) {
            var blockTag = CompoundBinaryTag.builder();
            blockTag.putInt("state", block.paletteIndex());
            blockTag.putIntArray("pos", new int[]{block.pos().blockX(), block.pos().blockY(), block.pos().blockZ()});
            if (block.blockEntity() != null)
                blockTag.put("nbt", block.blockEntity().data().putString("id", block.blockEntity().id()));
            blocks.add(blockTag.build());
        }
        root.put("blocks", blocks.build());

        // Entities
        root.put("entities", ListBinaryTag.from(schematic.entities()));

        return root.build();
    }

    private @NotNull Structure fromGenericSchematic(@NotNull Schematic schematic) {
        if (schematic instanceof Structure structure) return structure;
        // Otherwise do the conversion

        assertTrue(schematic.hasBlockData(), "schematic must have block data");
        var palettes = List.<Block[]>of(schematic.blockPalette().toArray(new Block[0]));

        //todo block data conversion

        return new Structure(schematic.size(), List.of(), palettes, schematic.entities());
    }

    private @NotNull ListBinaryTag writeBlockPalette(@NotNull Block[] palette) {
        var list = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
        for (var block : palette)
            list.add(writeBlockState(block));
        return list.build();
    }
}
