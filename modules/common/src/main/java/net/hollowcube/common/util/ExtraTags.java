package net.hollowcube.common.util;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponent;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class ExtraTags {
    private ExtraTags() {
    }

    public static @NotNull Tag<@NotNull Key> Key(@NotNull String key) {
        return Tag.String(key).map(
                str -> parseKey(str, null),
                Key::asString
        );
    }

    public static @NotNull Tag<Vec> VecAsList(@NotNull String key) {
        return Tag.Double(key).list().map(entries -> {
            double x = 0, y = 0, z = 0;
            if (entries.size() >= 1) x = entries.get(0);
            if (entries.size() >= 2) y = entries.get(1);
            if (entries.size() >= 3) z = entries.get(2);
            return new Vec(x, y, z);
        }, vec -> List.of(vec.x(), vec.y(), vec.z()));
    }

    @SuppressWarnings("UnstableApiUsage")
    public static <T> Tag<T> DataComponent(@NotNull String name, @NotNull DataComponent<T> component) {
        Supplier<Transcoder<BinaryTag>> factory = () -> new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
        return Tag.NBT(name).map(
                tag -> component.decode(factory.get(), tag).orElseThrow(),
                value -> component.encode(factory.get(), (T) value).orElseThrow()
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    public static <T> Tag<T> MappedView(@NotNull Tag<T> tag, @NotNull UnaryOperator<T> mapper) {
        return Tag.View(TagSerializer.fromCompound(
                compound -> mapper.apply(tag.read(compound)),
                value -> {
                    CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
                    tag.write(builder, mapper.apply(value));
                    return builder.build();
                }
        ));
    }

    public static @Nullable Key parseKey(String str, @Nullable Key defaultValue) {
        try {
            return Key.key(str.toLowerCase(Locale.ROOT));
        } catch (InvalidKeyException _) {
            return defaultValue;
        }
    }
}
