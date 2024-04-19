package net.hollowcube.mapmaker.map.util;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NbtUtil {

    public static @NotNull BinaryTag into(@NotNull Vec vec) {
        return ListBinaryTag.listBinaryTag(BinaryTagTypes.DOUBLE, List.of(
                DoubleBinaryTag.doubleBinaryTag(vec.x()),
                DoubleBinaryTag.doubleBinaryTag(vec.y()),
                DoubleBinaryTag.doubleBinaryTag(vec.z())
        ));
    }

    public static @Nullable Vec from(@Nullable BinaryTag nbt) {
        if (!(nbt instanceof ListBinaryTag list)) return null;
        double x = 0, y = 0, z = 0;
        if (list.size() >= 1) x = ((DoubleBinaryTag) list.get(0)).value();
        if (list.size() >= 2) y = ((DoubleBinaryTag) list.get(1)).value();
        if (list.size() >= 3) z = ((DoubleBinaryTag) list.get(2)).value();
        return new Vec(x, y, z);
    }

}
