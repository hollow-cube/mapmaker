package net.hollowcube.datafix;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public sealed interface DataType permits DataType.IdMapped {

    static @NotNull DataType dataType(@NotNull Key key) {
        return new DataTypeImpl(key.asString());
    }

    static @NotNull IdMapped idMappedDataType(@NotNull Key key) {
        return new DataTypeImpl(key.asString());
    }

    sealed interface IdMapped extends DataType permits DataTypeImpl {

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
     * Runtime only should never be stored or referenced across runtimes.
     */
    int id();

    @NotNull String name();

}
