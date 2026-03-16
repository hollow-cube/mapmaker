package net.hollowcube.datafix;

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

    protected void addReference(DataType.IdMapped type, String id) {
        // Noop for now, just marks the existence of this ID if thats helpful in the future
    }

    protected void addReference(DataType.IdMapped type, String id, DataType.BuilderFunc builder) {
        builder.apply(builderProxy(DataFixer.builderFor(type, id)));
    }

    protected void addReference(DataType type, DataType.BuilderFunc builder) {
        builder.apply(builderProxy(DataFixer.builderFor(type)));
    }

    protected void renameReference(DataType.IdMapped type, String oldId, String newId) {
        var oldBuilder = DataFixer.builderFor(type, oldId);
        var newBuilder = DataFixer.builderFor(type, newId);

        oldBuilder.properties.forEach(newBuilder::addProperty);
        oldBuilder.fixes.forEach(pair -> newBuilder.addFix(pair.first(), pair.second()));

        removeReference(type, oldId);
    }

    protected void removeReference(DataType.IdMapped type, String id) {
        // Noop for now, just marks the existence of this change if thats helpful in the future
    }

    // FIXES

    protected void addFix(DataType type, DataFix fix) {
        DataFixer.builderFor(type).addFix(encodedVersion, fix);
    }

    protected void addFix(DataType.IdMapped type, String id, DataFix fix) {
        DataFixer.builderFor(type, id).addFix(encodedVersion, fix);
    }

    private DataType.Builder builderProxy(DataTypeBuilder builder) {
        return new DataType.Builder() {
            @Override
            public DataType.Builder extend(DataType type) {
                builder.addProperty(new Property(new String[0], type, encodedVersion));
                return this;
            }

            @Override
            public DataType.Builder single(String path, DataType type) {
                builder.addProperty(new Property(path.split("\\."), type, encodedVersion));
                return this;
            }

            @Override
            public DataType.Builder list(String path, DataType type) {
                single(path, type);
                return this;
            }
        };
    }

}
