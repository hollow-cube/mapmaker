package net.hollowcube.schem.util;

import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTEnd;
import org.jglrxavpok.hephaistos.nbt.NBTType;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

public final class Adventure2Hephaistos {

    public static @NotNull NBT into(@NotNull BinaryTag tag) {
        return switch (tag) {
            case EndBinaryTag $ -> NBTEnd.INSTANCE;
            case ByteBinaryTag byteTag -> NBT.Byte(byteTag.value());
            case ShortBinaryTag shortTag -> NBT.Short(shortTag.value());
            case IntBinaryTag intTag -> NBT.Int(intTag.value());
            case LongBinaryTag longTag -> NBT.Long(longTag.value());
            case FloatBinaryTag floatTag -> NBT.Float(floatTag.value());
            case DoubleBinaryTag doubleTag -> NBT.Double(doubleTag.value());
            case ByteArrayBinaryTag byteArray -> NBT.ByteArray(byteArray.value());
            case StringBinaryTag string -> NBT.String(string.value());
            case ListBinaryTag list -> NBT.List(NBTType.byIndex(list.type().id()), list.size(), i -> into(list.get(i)));
            case CompoundBinaryTag compound -> compound(compound);
            case IntArrayBinaryTag intArray -> NBT.IntArray(intArray.value());
            case LongArrayBinaryTag longArray -> NBT.LongArray(longArray.value());
            default -> throw new IllegalArgumentException("Unknown tag type: " + tag.getClass().getSimpleName());
        };
    }

    public static @NotNull NBTCompound compound(@NotNull CompoundBinaryTag tag) {
        if (tag.size() == 0) return NBTCompound.EMPTY;

        var compound = new MutableNBTCompound();
        tag.forEach(entry -> compound.put(entry.getKey(), into(entry.getValue())));
        return compound.toCompound();
    }
}
