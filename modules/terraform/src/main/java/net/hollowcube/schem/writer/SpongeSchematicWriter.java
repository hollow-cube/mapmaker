package net.hollowcube.schem.writer;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.old.BlockUtil;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Writes schematics in the sponge schematic v3 format. Only v3 is supported for writing.
 *
 * @see <a href="https://github.com/SpongePowered/Schematic-Specification/tree/master">Format Specification</a>
 */
public class SpongeSchematicWriter implements SchematicWriter {
    public static final int FORMAT_VERSION = 3;

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

    private @NotNull CompoundBinaryTag createRoot(@NotNull Schematic schematic) {
        var root = CompoundBinaryTag.builder();
        root.putInt("Version", FORMAT_VERSION);
        root.putInt("DataVersion", gameData.dataVersion());
        root.put("Metadata", schematic.metadata().putString("Writer", NAME));

        var size = schematic.size();
        root.putShort("Width", (short) size.blockX());
        root.putShort("Height", (short) size.blockY());
        root.putShort("Length", (short) size.blockZ());

        var offset = schematic.offset();
        root.putIntArray("Offset", new int[]{offset.blockX(), offset.blockY(), offset.blockZ()});

        if (schematic.hasBlockData()) {
            root.put("Blocks", createBlockContainer(schematic));
        }

        if (schematic.hasBiomeData()) {
            root.put("Biomes", createBiomeContainer(schematic));
        }

        if (!schematic.entities().isEmpty()) {
            root.put("Entities", ListBinaryTag.from(schematic.entities()));
        }

        return CompoundBinaryTag.builder().put("Schematic", root.build()).build();
    }

    private @NotNull CompoundBinaryTag createBlockContainer(@NotNull Schematic schematic) {
        var container = CompoundBinaryTag.builder();

        var palette = schematic.blockPalette();
        var paletteTag = CompoundBinaryTag.builder();
        for (int i = 0; i < palette.size(); i++)
            paletteTag.putInt(BlockUtil.toStateString(palette.get(i)), i);
        container.put("Palette", paletteTag.build());
        container.put("Data", schematic.blockData());

        var blockEntityList = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
        for (var blockEntity : schematic.blockEntities()) {
            var pos = blockEntity.position();
            blockEntityList.add(CompoundBinaryTag.builder()
                    .putString("Id", blockEntity.id())
                    .putIntArray("Pos", new int[]{pos.blockX(), pos.blockY(), pos.blockZ()})
                    .put("Data", blockEntity.data())
                    .build());
        }
        // Note: we must include block entities or WorldEdit fails to read the schematic. Thank you worldedit!
        container.put("BlockEntities", blockEntityList.build());

        return container.build();
    }

    private @NotNull CompoundBinaryTag createBiomeContainer(@NotNull Schematic schematic) {
        var container = CompoundBinaryTag.builder();
        var palette = schematic.biomePalette();
        var paletteTag = CompoundBinaryTag.builder();
        for (int i = 0; i < palette.size(); i++)
            paletteTag.putInt(palette.get(i), i);
        container.put("Palette", paletteTag.build());
        container.put("Data", schematic.biomeData());
        return container.build();
    }
}
