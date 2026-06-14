package net.hollowcube.mapmaker.map.util;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    public static <T extends Enum<T>> @NotNull T readIntEnum(@Nullable BinaryTag tag, @NotNull Class<T> clazz) {
        if (tag instanceof IntBinaryTag intTag) {
            T[] constants = clazz.getEnumConstants();
            if (intTag.value() >= 0 && intTag.value() < constants.length) {
                return constants[intTag.value()];
            }
        }
        return clazz.getEnumConstants()[0];
    }

    @Contract("!null -> !null; null -> null")
    public static <T extends Enum<T>> @Nullable BinaryTag writeIntEnum(@Nullable T value) {
        return value != null ? IntBinaryTag.intBinaryTag(value.ordinal()) : null;
    }

    public static <T extends Enum<T>> @NotNull T readStringEnum(@Nullable BinaryTag tag, @NotNull Class<T> clazz) {
        if (tag instanceof StringBinaryTag string) {
            try {
                return Enum.valueOf(clazz, string.value().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException _) {
            }
        }
        return clazz.getEnumConstants()[0];
    }

    @Contract("!null -> !null; null -> null")
    public static <T extends Enum<T>> @Nullable BinaryTag writeStringEnum(@Nullable T value) {
        return value != null ? StringBinaryTag.stringBinaryTag(value.name().toLowerCase(Locale.ROOT)) : null;
    }

    public static <T> @NotNull RegistryKey<@NotNull T> readRegistryKey(@Nullable BinaryTag tag, @NotNull Function<Registries, Registry<T>> registry, @NotNull RegistryKey<@NotNull T> fallback) {
        if (tag instanceof StringBinaryTag string) {
            try {
                return Objects.requireNonNullElse(
                    registry.apply(MinecraftServer.process()).getKey(Key.key(string.value())),
                    fallback
                );
            } catch (InvalidKeyException _) {
            }
        }
        return fallback;
    }

    private NbtUtilV2() {
    }
}
