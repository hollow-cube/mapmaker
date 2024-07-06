package net.hollowcube.schem.writer;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

final class WriteHelpers {

    public static void assertTrue(boolean condition, @NotNull String message, @NotNull Object... args) {
        if (condition) return;
        throw new SchematicWriteException(MessageFormat.format(message, args));
    }

    public static @NotNull CompoundBinaryTag writeBlockState(@NotNull Block block) {
        var tag = CompoundBinaryTag.builder();
        tag.putString("Name", block.name());
        var properties = block.properties();
        if (!properties.isEmpty()) {
            var propsTag = CompoundBinaryTag.builder();
            for (var entry : properties.entrySet()) {
                propsTag.putString(entry.getKey(), entry.getValue());
            }
            tag.put("Properties", propsTag.build());
        }
        return tag.build();
    }

    public static long[] packPalette(int[] ints, int bitsPerEntry) {
        int intsPerLong = (int) Math.floor(64d / bitsPerEntry);
        long[] longs = new long[(int) Math.ceil(ints.length / (double) intsPerLong)];

        long mask = (1L << bitsPerEntry) - 1L;
        for (int i = 0; i < longs.length; i++) {
            for (int intIndex = 0; intIndex < intsPerLong; intIndex++) {
                int bitIndex = intIndex * bitsPerEntry;
                int intActualIndex = intIndex + i * intsPerLong;
                if (intActualIndex < ints.length) {
                    longs[i] |= (ints[intActualIndex] & mask) << bitIndex;
                }
            }
        }

        return longs;
    }

    // https://github.com/maruohon/litematica/blob/pre-rewrite/fabric/1.20.x/src/main/java/fi/dy/masa/litematica/schematic/container/LitematicaBitArray.java#L44
    //todo packTight

    private WriteHelpers() {
    }
}
