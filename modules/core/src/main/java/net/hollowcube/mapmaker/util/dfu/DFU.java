package net.hollowcube.mapmaker.util.dfu;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagSerializer;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.lang.reflect.Type;

public final class DFU {

    public static <T> @NotNull Tag<T> View(@NotNull Codec<T> codec) {
        return Tag.View(TagSerializer.fromCompound(
                compound -> codec.decode(NbtOps.INSTANCE, compound).result().orElseThrow().getFirst(),
                value -> (NBTCompound) codec.encode(value, NbtOps.INSTANCE, null).result().orElseThrow()
        )).defaultValue(() -> codec.decode(NbtOps.INSTANCE, NBTCompound.EMPTY).result().orElseThrow().getFirst());
    }

    public static <T, S extends JsonSerializer<T> & JsonDeserializer<T>> @NotNull S JsonSerializer(@NotNull Codec<T> codec) {
        //noinspection unchecked
        return (S) new CodecSerializer<>(codec);
    }

    private record CodecSerializer<T>(@NotNull Codec<T> codec) implements JsonSerializer<T>, JsonDeserializer<T> {

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return codec.decode(JsonOps.INSTANCE, json).result().orElseThrow().getFirst();
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            return codec.encode(src, JsonOps.INSTANCE, null).result().orElseThrow();
        }
    }
}
