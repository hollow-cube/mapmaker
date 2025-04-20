package net.hollowcube.datafix;

import org.jetbrains.annotations.NotNull;

public class Property {
    private final String[] path;
    private final DataType type;

    private final int fromVersion;
    private int toVersion;

    public Property(@NotNull String[] path, @NotNull DataType type, int fromVersion) {
        this.path = path;
        this.type = type;
        this.fromVersion = fromVersion;
    }

    public @NotNull String[] path() {
        return path;
    }

    public @NotNull DataType getType() {
        return type;
    }

    public int fromVersion() {
        return fromVersion;
    }

    public int toVersion() {
        return toVersion;
    }
}
