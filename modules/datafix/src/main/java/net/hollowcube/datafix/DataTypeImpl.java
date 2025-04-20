package net.hollowcube.datafix;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

final class DataTypeImpl implements DataType.IdMapped {
    public static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private final int id;
    private final String name;

    public DataTypeImpl(@NotNull String name) {
        this.id = ID_COUNTER.getAndIncrement();
        this.name = name;

        DataFixer.addDataType(this);
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataTypeImpl that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
