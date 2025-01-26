package net.hollowcube.mapmaker.map.util;

import net.kyori.adventure.nbt.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.kyori.adventure.nbt.FloatBinaryTag.floatBinaryTag;

public final class NbtUtilV2 {

    public static @NotNull Vec readFloat3(@Nullable BinaryTag tag) {
        if (tag instanceof ListBinaryTag list && list.size() == 3 && list.get(0) instanceof NumberBinaryTag) {
            return new Vec(list.getFloat(0), list.getFloat(1), list.getFloat(2));
        } else return Vec.ZERO;
    }

    public static @NotNull BinaryTag writeFloat3(@NotNull Point point) {
        return ListBinaryTag.listBinaryTag(BinaryTagTypes.FLOAT, List.of(
                floatBinaryTag((float) point.x()),
                floatBinaryTag((float) point.y()),
                floatBinaryTag((float) point.z())
        ));
    }

    public static @NotNull ItemStack readItemStack(@Nullable BinaryTag tag) {
        if (tag instanceof CompoundBinaryTag compound && compound.size() > 0 && compound.get("id") instanceof StringBinaryTag) {
            return ItemStack.fromItemNBT(compound);
        } else return ItemStack.AIR;
    }

    public static @NotNull BinaryTag writeItemStack(@NotNull ItemStack itemStack) {
        return itemStack.toItemNBT();
    }

    // Lists

    public static <T> void readListIndexed(@Nullable BinaryTag tag, T[] indices, @NotNull BiConsumer<T, BinaryTag> applicator) {
        if (!(tag instanceof ListBinaryTag list)) return;
        for (int i = 0; i < Math.min(list.size(), indices.length); i++) {
            applicator.accept(indices[i], list.get(i));
        }
    }

    public static <T> @NotNull BinaryTag writeListIndexed(T[] indices, @NotNull Function<T, BinaryTag> builder) {
        var list = ListBinaryTag.builder();
        for (T index : indices) list.add(builder.apply(index));
        return list.build();
    }

    private NbtUtilV2() {
    }
}
