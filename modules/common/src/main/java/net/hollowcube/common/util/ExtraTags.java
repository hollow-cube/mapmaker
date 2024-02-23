package net.hollowcube.common.util;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    private static final TagSerializer<Object> EMPTY_SERIALIZER = new TagSerializer<>() {
        @Override
        public @Nullable Object read(@NotNull TagReadable reader) {
            return null; // noop
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull Object value) {
            // noop
        }
    };

    /**
     * Returns a tag which does not serialize or deserialize.
     * <p>
     * Useful for storing transient references which would never need to be saved
     */
    public static <T> Tag<T> Transient(String name) {
        return (Tag<T>) Tag.Structure(name, EMPTY_SERIALIZER);
    }

}
