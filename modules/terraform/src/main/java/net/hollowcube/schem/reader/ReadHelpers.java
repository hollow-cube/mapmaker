package net.hollowcube.schem.reader;

import net.kyori.adventure.nbt.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.HashMap;

final class ReadHelpers {

    public static void assertTrue(boolean condition, @NotNull String message, @NotNull Object... args) {
        if (condition) return;
        throw new SchematicReadException(MessageFormat.format(message, args));
    }

    public static <T extends BinaryTag> T getRequired(@NotNull CompoundBinaryTag tag, @NotNull String key, @NotNull BinaryTagType<T> type) {
        var value = tag.get(key);
        if (value == null) throw new SchematicReadException("missing required field '" + key + "'");
        if (value.type() != type) throw new SchematicReadException("expected field '" + key + "' to be a " + type);
        //noinspection unchecked
        return (T) value;
    }

    public static @NotNull Point getRequiredPoint(@NotNull CompoundBinaryTag tag, @NotNull String key) {
        var rawOffset = getRequired(tag, key, BinaryTagTypes.INT_ARRAY);
        assertTrue(rawOffset.size() == 3, "invalid {0} size {1}", key, rawOffset.size());
        return new Vec(rawOffset.get(0), rawOffset.get(1), rawOffset.get(2));
    }

    public static @NotNull Point getRequiredVec3(CompoundBinaryTag tag, String key) {
        var vec = key.isEmpty() ? tag : getRequired(tag, key, BinaryTagTypes.COMPOUND);
        var width = getRequired(vec, "x", BinaryTagTypes.INT).value();
        var height = getRequired(vec, "y", BinaryTagTypes.INT).value();
        var length = getRequired(vec, "z", BinaryTagTypes.INT).value();
        return new Vec(width, height, length);
    }

    public static @NotNull Block readBlockState(@NotNull CompoundBinaryTag tag) {
        var name = getRequired(tag, "Name", BinaryTagTypes.STRING).value();
        var block = Block.fromNamespaceId(name);
        assertTrue(block != null, "unknown block: {0}", name);

        var propsTag = tag.getCompound("Properties");
        if (propsTag.size() == 0) return block;
        var properties = new HashMap<String, String>();
        for (var entry : propsTag) {
            assertTrue(entry.getValue().type() == BinaryTagTypes.STRING, "expected property value to be a string");
            properties.put(entry.getKey(), ((StringBinaryTag) entry.getValue()).value());
        }
        try {
            return block.withProperties(properties);
        } catch (IllegalArgumentException e) {
            throw new SchematicReadException("failed to parse block properties", e);
        }
    }

    public static void unpackPalette(int[] out, long[] in, int bitsPerEntry) {
        assert in.length != 0 : "unpack input array is zero";

        var intsPerLong = Math.floor(64d / bitsPerEntry);
        var intsPerLongCeil = (int) Math.ceil(intsPerLong);

        long mask = (1L << bitsPerEntry) - 1L;
        for (int i = 0; i < out.length; i++) {
            int longIndex = i / intsPerLongCeil;
            int subIndex = i % intsPerLongCeil;

            out[i] = (int) ((in[longIndex] >>> (bitsPerEntry * subIndex)) & mask);
        }
    }

    // https://github.com/maruohon/litematica/blob/pre-rewrite/fabric/1.20.x/src/main/java/fi/dy/masa/litematica/schematic/container/LitematicaBitArray.java#L63
    public static void unpackPaletteTight(int[] out, long[] in, int bitsPerEntry) {
        assert in.length != 0 : "unpack input array is zero";

        long maxEntryValue = (1L << bitsPerEntry) - 1L;
        for (int i = 0; i < out.length; i++) {
            long startOffset = i * (long) bitsPerEntry;
            int startArrIndex = (int) (startOffset >> 6);
            int endArrIndex = (int) (((i + 1L) * (long) bitsPerEntry - 1L) >> 6);
            int subIndex = (int) (startOffset & 0x3F);

            if (startArrIndex == endArrIndex) {
                out[i] = (int) (in[startArrIndex] >>> subIndex & maxEntryValue);
            } else {
                out[i] = (int) ((in[startArrIndex] >>> subIndex | in[endArrIndex] << (64 - subIndex)) & maxEntryValue);
            }
        }
    }

    private ReadHelpers() {
    }
}
