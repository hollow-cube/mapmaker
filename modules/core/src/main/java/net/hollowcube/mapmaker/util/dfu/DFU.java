package net.hollowcube.mapmaker.util.dfu;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagSerializer;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.lang.reflect.Type;
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
                compound -> unwrap(codec.decode(NbtOps.INSTANCE, compound).map(Pair::getFirst)),
                value -> (NBTCompound) unwrap(codec.encode(value, NbtOps.INSTANCE, null))
        );
    }

    private static <T> @NotNull Supplier<T> codecEmptySupplier(@NotNull Codec<T> codec) {
        return () -> unwrap(codec.decode(NbtOps.INSTANCE, NBTCompound.EMPTY).map(Pair::getFirst));
    }

    public static <T, S extends JsonSerializer<T> & JsonDeserializer<T>> @NotNull S JsonSerializer(@NotNull Codec<T> codec) {
        //noinspection unchecked
        return (S) new CodecJsonSerializer<>(codec);
    }

    private record CodecJsonSerializer<T>(@NotNull Codec<T> codec) implements JsonSerializer<T>, JsonDeserializer<T> {

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return codec.decode(JsonOps.INSTANCE, json).result().orElseThrow().getFirst();
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            return codec.encode(src, JsonOps.INSTANCE, null).result().orElseThrow();
        }
    }

    private static <T> @NotNull T unwrap(@NotNull DataResult<T> result) {
        if (result.result().isPresent()) {
            return result.result().get();
        } else if (result.error().isPresent()) {
            throw new RuntimeException(result.error().get().message());
        } else {
            throw new RuntimeException("Unknown error");
        }
    }
}
