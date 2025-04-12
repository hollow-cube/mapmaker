package net.hollowcube.datafix;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class DataVersion {
    public static Object convert(@NotNull DataType.IdMapped type, @NotNull Map<String, Object> object, int fromVersion, int toVersion) {
        final Object rawId = object.get("id");
        if (!(rawId instanceof String id)) return object;

        final DataTypeImpl.IdMapped typeImpl = (DataTypeImpl.IdMapped) type;
        for (int i = fromVersion + 1; i <= toVersion; i++) {
            for (var fix : typeImpl.fixes.getOrDefault(i, List.of())) {
                object = fix.apply(object);
            }
        }

        return object;
    }

    private final int version;

    protected DataVersion(int version) {
        this.version = version;
    }

    @FunctionalInterface
    public interface FieldEditor {
        @NotNull Field edit(@NotNull Field field);
    }

    public interface Field {
        @NotNull Field single(@NotNull String path, @NotNull DataType type);
        @NotNull Field list(@NotNull String path, @NotNull DataType type);
    }

    protected void addReference(DataType.IdMapped type, @NotNull String id) {
        // Noop for now, just marks the existence of this ID if thats helpful in the future
    }

    protected void addReference(DataType.IdMapped type, @NotNull String id, @NotNull FieldEditor editor) {
        final DataTypeImpl typeImpl = ((DataTypeImpl.IdMapped) type).named(id);
        editor.edit(new Field() {
            @Override
            public @NotNull Field single(@NotNull String path, @NotNull DataType type) {
                typeImpl.fields.add(new DataTypeImpl.FieldImpl(path, type, false, version));
                return this;
            }

            @Override
            public @NotNull Field list(@NotNull String path, @NotNull DataType type) {
                typeImpl.fields.add(new DataTypeImpl.FieldImpl(path, type, true, version));
                return this;
            }
        });
    }


    protected void addReference(DataType type, @NotNull String path, @NotNull DataType other) {

    }

    protected void addFix(@NotNull DataType.IdMapped type, @NotNull String id, @NotNull Function<Map<String, Object>, Map<String, Object>> fix) {
        ((DataTypeImpl.IdMapped) type).fixes.computeIfAbsent(this.version, _ -> new ArrayList<>()).add(fix);
    }

}
