package net.hollowcube.mapmaker.util;

import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TagUtil {
    private TagUtil() {
    }

    public static <T> Tag<T> noop(String name) {
        return Tag.Structure(name, new TagSerializer<>() {
            @Override
            public @Nullable T read(@NotNull TagReadable reader) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(@NotNull TagWritable writer, @NotNull T value) {
                throw new UnsupportedOperationException();
            }
        });
    }

}
