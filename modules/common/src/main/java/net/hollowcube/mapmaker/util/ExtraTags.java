package net.hollowcube.mapmaker.util;

import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExtraTags {
    private ExtraTags() {
    }

    /**
     * Returns a tag which does not serialize or deserialize.
     * <p>
     * Useful for storing transient references which would never need to be saved
     * (eg a reference to a players {@link net.hollowcube.mapmaker.model.PlayerData})
     */
    public static <T> Tag<T> Transient(String name) {
        return Tag.Structure(name, new TagSerializer<>() {
            @Override
            public @Nullable T read(@NotNull TagReadable reader) {
                return null; // noop
            }

            @Override
            public void write(@NotNull TagWritable writer, @NotNull T value) {
                // noop
            }
        });
    }

}
