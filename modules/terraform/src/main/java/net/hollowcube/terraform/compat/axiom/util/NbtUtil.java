package net.hollowcube.terraform.compat.axiom.util;

import net.kyori.adventure.nbt.*;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NbtUtil {

    public static @NotNull CompoundBinaryTag mergeNbtCompounds(@NotNull CompoundBinaryTag left, @NotNull CompoundBinaryTag right) {
        var result = CompoundBinaryTag.builder().put(left);
        for (var entry : right) {
            var leftTag = left.get(entry.getKey());

            // Not conflicting, insert right
            if (leftTag == null) {
                result.put(entry.getKey(), entry.getValue());
                continue;
            }

            // Both compounds, merge deeper
            if (leftTag instanceof CompoundBinaryTag leftCompound && entry.getValue() instanceof CompoundBinaryTag rightCompound) {
                result.put(entry.getKey(), mergeNbtCompounds(leftCompound, rightCompound));
                continue;
            }

            // They are not both compounds, take right always
            result.put(entry.getKey(), entry.getValue());
        }
        return result.build();
    }

    public static @Nullable Pos readSpawnPosition(@NotNull CompoundBinaryTag tag) {
        var posTag = tag.getList("Pos", BinaryTagTypes.DOUBLE);
        if (posTag.size() < 3) return null;
        var rotationTag = tag.getList("Rotation", BinaryTagTypes.FLOAT);
        return new Pos(
                posTag.getDouble(0),
                posTag.getDouble(1),
                posTag.getDouble(2),
                rotationTag.size() < 2 ? 0 : rotationTag.getFloat(0),
                rotationTag.size() < 2 ? 0 : rotationTag.getFloat(1)
        );
    }

    public static @NotNull ListBinaryTag toPosTag(@NotNull Pos pos) {
        return ListBinaryTag.builder()
                .add(DoubleBinaryTag.doubleBinaryTag(pos.x()))
                .add(DoubleBinaryTag.doubleBinaryTag(pos.y()))
                .add(DoubleBinaryTag.doubleBinaryTag(pos.z()))
                .build();
    }

    public static @NotNull ListBinaryTag toRotationTag(@NotNull Pos pos) {
        return ListBinaryTag.builder()
                .add(FloatBinaryTag.floatBinaryTag(pos.yaw()))
                .add(FloatBinaryTag.floatBinaryTag(pos.pitch()))
                .build();
    }
}
