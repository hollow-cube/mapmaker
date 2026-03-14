package net.hollowcube.common.util.dfu;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagSerializer;

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
    public static <T> Tag<T> Tag(Codec<T> codec, String key) {
        return Tag.Structure(key, codecTagSerializer(codec)).defaultValue(codecEmptySupplier(codec));
    }

    public static <T> Tag<T> View(Codec<T> codec) {
        return Tag.View(codecTagSerializer(codec)).defaultValue(codecEmptySupplier(codec));
    }

    public static <T> TagSerializer<T> codecTagSerializer(Codec<T> codec) {
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

    public static <T> CompoundBinaryTag encodeNbt(Codec<T> codec, T value) {
        var coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
        return (CompoundBinaryTag) codec.encode(coder, value).orElseThrow();
    }

    private static <T> Supplier<T> codecEmptySupplier(Codec<T> codec) {
        return () -> codec.decode(Transcoder.NBT, CompoundBinaryTag.empty()).orElseThrow();
    }
}
