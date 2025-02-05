package net.hollowcube.common.util.dfu;

import com.mojang.serialization.Codec;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public final class DFU {

    /**
     * Retuns a (default empty if missing) tag for the given codec.
     *
     * <p>The tag must have default values for every field and be an object at the root.
     * The requirement to be an option is intentional as it allows extension in the future
     * without any complex data fixing (even though the library supports it).</p>
     *
     * @param codec the codec to use
     * @param key   the key to use for the tag
     * @param <T>   the type of the codec
     * @return a tag for the given codec
     */
    public static <T> @NotNull Tag<T> Tag(@NotNull Codec<T> codec, @NotNull String key) {
        return Tag.Structure(key, codecTagSerializer(codec)).defaultValue(codecEmptySupplier(codec));
    }

    public static <T> @NotNull Tag<T> View(@NotNull Codec<T> codec) {
        return Tag.View(codecTagSerializer(codec)).defaultValue(codecEmptySupplier(codec));
    }

    private static <T> @NotNull TagSerializer<T> codecTagSerializer(@NotNull Codec<T> codec) {
        return TagSerializer.fromCompound(
                compound -> codec.parse(NbtOps.INSTANCE, compound).getOrThrow(),
                value -> (CompoundBinaryTag) codec.encode(value, NbtOps.INSTANCE, null).getOrThrow()
        );
    }

    public static <T> @NotNull CompoundBinaryTag encodeNbt(@NotNull Codec<T> codec, @NotNull T value) {
        return (CompoundBinaryTag) codec.encode(value, NbtOps.INSTANCE, null).getOrThrow();
    }

    private static <T> @NotNull Supplier<T> codecEmptySupplier(@NotNull Codec<T> codec) {
        return () -> codec.parse(NbtOps.INSTANCE, CompoundBinaryTag.empty()).getOrThrow();
    }
}
