package net.hollowcube.datafix;

import net.kyori.adventure.key.Key;

public sealed interface DataType permits DataType.IdMapped {

    static DataType dataType(Key key) {
        return new DataTypeImpl(key.asString());
    }

    static IdMapped idMappedDataType(Key key) {
        return new DataTypeImpl(key.asString());
    }

    sealed interface IdMapped extends DataType permits DataTypeImpl {

    }

    interface Builder {
        Builder extend(DataType type);

        Builder single(String path, DataType type);
        Builder list(String path, DataType type);
    }

    @FunctionalInterface
    interface BuilderFunc {
        Builder apply(Builder type);
    }

    /**
     * Runtime only should never be stored or referenced across runtimes.
     */
    int id();

    String name();

}
