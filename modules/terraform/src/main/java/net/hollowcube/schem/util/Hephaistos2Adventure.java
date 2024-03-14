package net.hollowcube.schem.util;

import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.*;

import java.util.ArrayList;

public class Hephaistos2Adventure {

    public static @NotNull BinaryTag into(@NotNull NBT tag) {
        return switch (tag) {
            case NBTEnd $ -> EndBinaryTag.endBinaryTag();
            case NBTByte byteTag -> ByteBinaryTag.byteBinaryTag(byteTag.getValue());
            case NBTShort shortTag -> ShortBinaryTag.shortBinaryTag(shortTag.getValue());
            case NBTInt intTag -> IntBinaryTag.intBinaryTag(intTag.getValue());
            case NBTLong longTag -> LongBinaryTag.longBinaryTag(longTag.getValue());
            case NBTFloat floatTag -> FloatBinaryTag.floatBinaryTag(floatTag.getValue());
            case NBTDouble doubleTag -> DoubleBinaryTag.doubleBinaryTag(doubleTag.getValue());
            case NBTByteArray byteArray -> ByteArrayBinaryTag.byteArrayBinaryTag(byteArray.getValue().copyArray());
            case NBTString string -> StringBinaryTag.stringBinaryTag(string.getValue());
            case NBTList<?> list -> {
                var entries = new ArrayList<BinaryTag>();
                for (var value : list) entries.add(into(value));
                yield ListBinaryTag.listBinaryTag(TYPE_BY_ID[list.getSubtagType().getOrdinal()], entries);
            }
            case NBTCompound compound -> compound(compound);
            case NBTIntArray intArray -> IntArrayBinaryTag.intArrayBinaryTag(intArray.getValue().copyArray());
            case NBTLongArray longArray -> LongArrayBinaryTag.longArrayBinaryTag(longArray.getValue().copyArray());
            default -> throw new IllegalArgumentException("Unknown tag type: " + tag.getClass().getSimpleName());
        };
    }

    public static @NotNull CompoundBinaryTag compound(@NotNull NBTCompound tag) {
        var compound = CompoundBinaryTag.builder();
        tag.forEach((key, value) -> compound.put(key, into(value)));
        return compound.build();
    }

    private static final BinaryTagType<?>[] TYPE_BY_ID = new BinaryTagType[]{
            BinaryTagTypes.END,
            BinaryTagTypes.BYTE,
            BinaryTagTypes.SHORT,
            BinaryTagTypes.INT,
            BinaryTagTypes.LONG,
            BinaryTagTypes.FLOAT,
            BinaryTagTypes.DOUBLE,
            BinaryTagTypes.BYTE_ARRAY,
            BinaryTagTypes.STRING,
            BinaryTagTypes.LIST,
            BinaryTagTypes.COMPOUND,
            BinaryTagTypes.INT_ARRAY,
            BinaryTagTypes.LONG_ARRAY
    };
}
