package net.hollowcube.mapmaker.util.dfu;

import com.mojang.serialization.Codec;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagSerializer;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

public final class DFU {

    public static <T> @NotNull Tag<T> View(@NotNull Codec<T> codec) {
        return Tag.View(TagSerializer.fromCompound(
                compound -> codec.decode(NbtOps.INSTANCE, compound).result().orElseThrow().getFirst(),
                value -> (NBTCompound) codec.encode(value, NbtOps.INSTANCE, null).result().orElseThrow()
        )).defaultValue(() -> codec.decode(NbtOps.INSTANCE, NBTCompound.EMPTY).result().orElseThrow().getFirst());
    }
}
