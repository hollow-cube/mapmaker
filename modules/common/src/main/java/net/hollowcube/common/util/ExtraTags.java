package net.hollowcube.common.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponent;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.nbt.BinaryTagSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public final class ExtraTags {
    private ExtraTags() {
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
        Supplier<BinaryTagSerializer.Context> factory = () -> new BinaryTagSerializer.ContextWithRegistries(MinecraftServer.process());
        return Tag.NBT(name).map(
                tag -> component.read(factory.get(), tag),
                value -> component.write(factory.get(), value)
        );
    }

}
