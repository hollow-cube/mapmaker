package net.hollowcube.common.util.dfu;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
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

    public static <T> @NotNull TagSerializer<T> codecTagSerializer(@NotNull Codec<T> codec) {
        var coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
        return TagSerializer.fromCompound(
                compound -> codec.decode(coder, compound).orElseThrow(),
                value -> {
                    var tag = codec.encode(coder, value).orElseThrow();
                    if (tag instanceof CompoundBinaryTag compound) return compound;
                    return CompoundBinaryTag.empty();
                }
        );
    }

    public static <T> @NotNull CompoundBinaryTag encodeNbt(@NotNull Codec<T> codec, @NotNull T value) {
        var coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
        return (CompoundBinaryTag) codec.encode(coder, value).orElseThrow();
    }

    private static <T> @NotNull Supplier<T> codecEmptySupplier(@NotNull Codec<T> codec) {
        return () -> codec.decode(Transcoder.NBT, CompoundBinaryTag.empty()).orElseThrow();
    }
}
