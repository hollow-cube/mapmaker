package net.hollowcube.terraform.instance;


import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.collections.ImmutableByteArray;
import org.jglrxavpok.hephaistos.nbt.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Simple schematic file reader.
 */
public class SchematicReader {

    public static @NotNull Schematic read(@NotNull InputStream stream) throws IOException {
        try (var reader = new NBTReader(stream, CompressedProcesser.GZIP)) {
            return read(reader);
        }
    }

    public static @NotNull Schematic read(@NotNull Path path) throws IOException {
        try (var reader = new NBTReader(path, CompressedProcesser.GZIP)) {
            return read(reader);
        }
    }

    @ApiStatus.Internal
    public static @NotNull Schematic read(@NotNull NBTReader reader) throws IOException {
        try {
            NBTCompound tag = (NBTCompound) reader.read();

            Short width = tag.getShort("Width");
            Check.notNull(width, "Missing required field 'Width'");
            Short height = tag.getShort("Height");
            Check.notNull(height, "Missing required field 'Height'");
            Short length = tag.getShort("Length");
            Check.notNull(length, "Missing required field 'Length'");

            NBTCompound metadata = tag.getCompound("Metadata");
            Check.notNull(metadata, "Missing required field 'Metadata'");

            Integer offsetX = metadata.getInt("WEOffsetX");
            Check.notNull(offsetX, "Missing required field 'Metadata.WEOffsetX'");
            Integer offsetY = metadata.getInt("WEOffsetY");
            Check.notNull(offsetY, "Missing required field 'Metadata.WEOffsetY'");
            Integer offsetZ = metadata.getInt("WEOffsetZ");
            Check.notNull(offsetZ, "Missing required field 'Metadata.WEOffsetZ'");

            NBTCompound palette = tag.getCompound("Palette");
            Check.notNull(palette, "Missing required field 'Palette'");
            ImmutableByteArray blockArray = tag.getByteArray("BlockData");
            Check.notNull(blockArray, "Missing required field 'BlockData'");

            Integer paletteSize = tag.getInt("PaletteMax");
            Check.notNull(paletteSize, "Missing required field 'PaletteMax'");

            Block[] paletteBlocks = new Block[paletteSize];

            ArgumentBlockState state = new ArgumentBlockState("");
            palette.forEach((key, value) -> {
                int assigned = ((NBTInt) value).getValue();
                Block block = state.parse(key);
                paletteBlocks[assigned] = block;
            });

            return new Schematic(
                    new Vec(width, height, length),
                    new Vec(offsetX, offsetY, offsetZ),
                    paletteBlocks,
                    blockArray.copyArray()
            );
        } catch (NullPointerException | NBTException e) {
            throw new RuntimeException("Invalid schematic file", e);
        }
    }

}
