package net.hollowcube.mapmaker.map.util;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTDouble;
import org.jglrxavpok.hephaistos.nbt.NBTList;
import org.jglrxavpok.hephaistos.nbt.NBTType;

import java.util.List;

public final class NbtUtil {

    public static @NotNull NBT into(@NotNull Vec vec) {
        return new NBTList<>(NBTType.TAG_Double, List.of(
                new NBTDouble(vec.x()),
                new NBTDouble(vec.y()),
                new NBTDouble(vec.z())
        ));
    }

    public static @Nullable Vec from(@Nullable NBT nbt) {
        if (!(nbt instanceof NBTList<?> list)) return null;
        double x = 0, y = 0, z = 0;
        if (list.getSize() >= 1) x = ((NBTDouble) list.get(0)).getValue();
        if (list.getSize() >= 2) y = ((NBTDouble) list.get(1)).getValue();
        if (list.getSize() >= 3) z = ((NBTDouble) list.get(2)).getValue();
        return new Vec(x, y, z);
    }

}
