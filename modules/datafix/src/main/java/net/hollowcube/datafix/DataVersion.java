package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

// basically dsl for writing fixes, nothing else.
public abstract class DataVersion {
    private final int encodedVersion; // version << 8 | subversion

    protected DataVersion(int version) {
        this(version, 0);
    }

    protected DataVersion(int version, int subversion) {
        this.encodedVersion = (version << 8) | (subversion & 0xFF);
    }

    // REFERENCES

    protected void addReference(DataType.IdMapped type, @NotNull String id) {
        // Noop for now, just marks the existence of this ID if thats helpful in the future
    }

    protected void addReference(DataType.IdMapped type, @NotNull String id, @NotNull DataType.BuilderFunc builder) {
        builder.apply(builderProxy(DataFixer.builderFor(type, id)));
    }

    protected void addReference(DataType type, @NotNull DataType.BuilderFunc builder) {
        builder.apply(builderProxy(DataFixer.builderFor(type)));
    }

    protected void renameReference(DataType.IdMapped type, @NotNull String oldId, @NotNull String newId) {
        // TODO
    }

    protected void removeReference(DataType.IdMapped type, @NotNull String id) {
        // Noop for now, just marks the existence of this change if thats helpful in the future
    }


    // FIXES

    protected void addFix(@NotNull DataType type, @NotNull Function<Value, Value> fix) {
        DataFixer.builderFor(type).addFix(encodedVersion, fix);
    }

    protected void addFix(@NotNull DataType.IdMapped type, @NotNull String id, @NotNull Function<Value, Value> fix) {
        DataFixer.builderFor(type, id).addFix(encodedVersion, fix);
    }

    private DataType.Builder builderProxy(@NotNull DataTypeBuilder builder) {
        return new DataType.Builder() {
            @Override
            public DataType.@NotNull Builder extend(@NotNull DataType type) {
                builder.addProperty(new Property(new String[0], type, encodedVersion));
                return this;
            }

            @Override
            public DataType.@NotNull Builder single(@NotNull String path, @NotNull DataType type) {
                builder.addProperty(new Property(path.split("\\."), type, encodedVersion));
                return this;
            }

            @Override
            public DataType.@NotNull Builder list(@NotNull String path, @NotNull DataType type) {
                single(path, type);
                return this;
            }
        };
    }

}
