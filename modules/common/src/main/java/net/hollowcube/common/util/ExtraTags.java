package net.hollowcube.common.util;

import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExtraTags {
    private ExtraTags() {
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
