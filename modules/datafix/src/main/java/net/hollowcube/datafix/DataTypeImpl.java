package net.hollowcube.datafix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class DataTypeImpl implements DataType {
    private final String name;
    final List<FieldImpl> fields; //todo private
    final Map<Integer, List<Function<Map<String, Object>, Map<String, Object>>>> fixes = new HashMap<>();

    public DataTypeImpl(@NotNull String name) {
        this.name = name;
        this.fields = new ArrayList<>();
    }

    public @NotNull String stringify() {
        return this + " {\n\t" +
                fields.stream()
                        .map(FieldImpl::toString)
                        .reduce((a, b) -> a + "\n\t" + b)
                        .orElse("") +
                "\n}";
    }

    @Override
    public String toString() {
        return "@" + name;
    }

    public static class IdMapped extends DataTypeImpl implements DataType.IdMapped {
        private final Map<String, DataTypeImpl> idMap = new HashMap<>();

        public IdMapped(@NotNull String name) {
            super(name);
        }

        public @NotNull DataTypeImpl named(@NotNull String id) {
            return idMap.computeIfAbsent(id, DataTypeImpl::new);
        }

        public @Nullable DataTypeImpl namedOrNull(@NotNull String id) {
            return idMap.get(id);
        }

        public void forEach(@NotNull Consumer<DataTypeImpl> fn) {
            for (var entry : idMap.values()) {
                fn.accept(entry);
            }
        }

        @Override
        public @NotNull String stringify() {
            return idMap.entrySet().stream()
                    .map(entry -> this + "/" + entry.getValue().stringify().substring(1))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        }
    }

    record FieldImpl(@NotNull String path, @NotNull DataType type, boolean isList, int startVersion) {
        @Override
        public String toString() {
            return path + ": " + (isList ? "[]" : "") + type + " >" + startVersion;
        }
    }
}
