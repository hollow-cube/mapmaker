package net.hollowcube.datafix;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public sealed interface DataType extends Keyed permits DataType.IdMapped, DataTypeImpl {

    static @NotNull DataType dataType(@NotNull Key key) {
        return new DataTypeImpl(key);
    }

    static @NotNull IdMapped idMappedDataType(@NotNull Key key) {
        return new DataTypeIDMappedImpl(key);
    }

    sealed interface IdMapped extends DataType permits DataTypeIDMappedImpl {

    }

    interface Builder {
        @NotNull Builder extend(@NotNull DataType type);

        @NotNull Builder single(@NotNull String path, @NotNull DataType type);
        @NotNull Builder list(@NotNull String path, @NotNull DataType type);
    }

    @FunctionalInterface
    interface BuilderFunc {
        @NotNull Builder apply(@NotNull Builder type);
    }

    /**
     * <p>Upgrades the given object from one version to another.</p>
     *
     * <p>Passing the same or a prior version in toVersion is a noop.</p>
     *
     * @param coder The transcoder instance for the given data type.
     * @param object The object to upgrade, for example
     * @param fromVersion The current version of the data, or 0 to run all upgrades (can make sense if you don't have a better educated guess).
     * @param toVersion The target version of the data.
     * @return The upgraded object.
     */
    default <T> T upgrade(@NotNull Transcoder<T> coder, @NotNull T object, int fromVersion, int toVersion) {
        throw new UnsupportedOperationException("todo");
    }

}
