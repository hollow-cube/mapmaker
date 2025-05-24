package net.hollowcube.datafix;

import org.jetbrains.annotations.NotNull;

// basically dsl for writing fixes, nothing else.
public abstract class DataVersion {
    private final int encodedVersion; // version << 8 | subversion

    protected DataVersion(int version) {
        this(version, 0);
    }

    protected DataVersion(int version, int subversion) {
        this.encodedVersion = (version << 8) | (subversion & 0xFF);
    }

    public int version() {
        return encodedVersion >> 8;
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
        var oldBuilder = DataFixer.builderFor(type, oldId);
        var newBuilder = DataFixer.builderFor(type, newId);

        oldBuilder.properties.forEach(newBuilder::addProperty);
        oldBuilder.fixes.forEach(pair -> newBuilder.addFix(pair.first(), pair.second()));

        removeReference(type, oldId);
    }

    protected void removeReference(DataType.IdMapped type, @NotNull String id) {
        // Noop for now, just marks the existence of this change if thats helpful in the future
    }


    // FIXES

    protected void addFix(@NotNull DataType type, @NotNull DataFix fix) {
        DataFixer.builderFor(type).addFix(encodedVersion, fix);
    }

    protected void addFix(@NotNull DataType.IdMapped type, @NotNull String id, @NotNull DataFix fix) {
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
