package net.hollowcube.datafix;

public class Property {
    private final String[] path;
    private final DataType type;

    private final int fromVersion;
    private int toVersion;

    public Property(String[] path, DataType type, int fromVersion) {
        this.path = path;
        this.type = type;
        this.fromVersion = fromVersion;
    }

    public String[] path() {
        return path;
    }

    public DataType getType() {
        return type;
    }

    public int fromVersion() {
        return fromVersion;
    }

    public int toVersion() {
        return toVersion;
    }
}
