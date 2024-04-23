package net.hollowcube.mapmaker.map.util;

import net.kyori.adventure.nbt.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NbtUtil {

    public static @NotNull BinaryTag into(@NotNull Point vec) {
        return ListBinaryTag.listBinaryTag(BinaryTagTypes.DOUBLE, List.of(
                DoubleBinaryTag.doubleBinaryTag(vec.x()),
                DoubleBinaryTag.doubleBinaryTag(vec.y()),
                DoubleBinaryTag.doubleBinaryTag(vec.z())
        ));
    }

    public static @Nullable Vec from(@Nullable BinaryTag nbt) {
        if (!(nbt instanceof ListBinaryTag list)) return null;
        return from(list);
    }

    public static @NotNull Vec from(@NotNull ListBinaryTag list) {
        double x = 0, y = 0, z = 0;
        if (list.size() >= 1) x = ((DoubleBinaryTag) list.get(0)).value();
        if (list.size() >= 2) y = ((DoubleBinaryTag) list.get(1)).value();
        if (list.size() >= 3) z = ((DoubleBinaryTag) list.get(2)).value();
        return new Vec(x, y, z);
    }

    public static @NotNull Pos readRotation(@NotNull Point pos, ListBinaryTag rot) {
        float yaw = 0, pitch = 0;
        if (rot.size() >= 1) yaw = ((FloatBinaryTag) rot.get(0)).value();
        if (rot.size() >= 2) pitch = ((FloatBinaryTag) rot.get(1)).value();
        return new Pos(pos, yaw, pitch);
    }

    public static @NotNull BinaryTag writeRotation(@NotNull Pos pos) {
        return ListBinaryTag.listBinaryTag(BinaryTagTypes.FLOAT, List.of(
                FloatBinaryTag.floatBinaryTag(pos.yaw()),
                FloatBinaryTag.floatBinaryTag(pos.pitch())
        ));
    }

    public static <E extends Enum<E>> @NotNull E readEnum(@NotNull CompoundBinaryTag tag, @NotNull String key, @NotNull E defaultValue) {
        return switch (tag.get(key)) {
            case StringBinaryTag string -> Enum.valueOf(defaultValue.getDeclaringClass(), string.value().toUpperCase());
            case NumberBinaryTag number -> defaultValue.getDeclaringClass().getEnumConstants()[number.intValue()];
            case null, default -> defaultValue;
        };
    }
}
