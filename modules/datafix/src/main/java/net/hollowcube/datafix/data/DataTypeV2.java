package net.hollowcube.datafix.data;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public sealed interface DataTypeV2 permits DataTypeV2.IdMapped {

    static @NotNull DataTypeV2 dataType(@NotNull Key key) {
        return new DataTypeV2Impl(key.asString());
    }

    static @NotNull DataTypeV2.IdMapped idMappedDataType(@NotNull Key key) {
        return new DataTypeV2Impl(key.asString());
    }

    sealed interface IdMapped extends DataTypeV2 permits DataTypeV2Impl {

    }

    /**
     * Runtime only should never be stored or referenced across runtimes.
     */
    int id();

    @NotNull String name();

}
