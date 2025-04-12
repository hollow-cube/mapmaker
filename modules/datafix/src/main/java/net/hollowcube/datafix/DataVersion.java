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

        DataTypeImpl typeImpl = ((DataTypeImpl.IdMapped) type).named(id);
        for (int i = fromVersion + 1; i <= toVersion; i++) {
            for (var fix : typeImpl.fixes.getOrDefault(i, List.of())) {
                object = fix.apply(object);
            }

            typeImpl = ((DataTypeImpl.IdMapped) type).named((String) object.get("id"));
            for (var field : typeImpl.fields) {
                if (i < field.startVersion()) continue;

                var a = object.get(field.path());
                if (a == null) continue;
                if (field.isList()) {
                    if (!(a instanceof List<?> list)) continue;

                    var newList = new ArrayList<>();
                    for (var item : list) {
                        if (!(item instanceof Map<?, ?> map)) continue;
                        newList.add(convert((DataType.IdMapped) field.type(), (Map<String, Object>) map, fromVersion, toVersion));
                    }
                    object.put(field.path(), newList);
                } else {
                    // todo
                }
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
        @NotNull Field extend(@NotNull DataType type);
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

    protected void renameReference(DataType.IdMapped type, @NotNull String oldId, @NotNull String newId) {
        var typeImpl = ((DataTypeImpl.IdMapped) type);
        typeImpl.named(newId).fields.addAll(typeImpl.named(oldId).fields);
    }

    protected void removeReference(DataType.IdMapped type, @NotNull String id) {
    }


    protected void addReference(DataType type, @NotNull FieldEditor editor) {

    }

    protected void addFix(@NotNull DataType.IdMapped type, @NotNull Function<Map<String, Object>, Map<String, Object>> fix) {
        ((DataTypeImpl.IdMapped) type).forEach(t -> t.fixes.computeIfAbsent(this.version, _ -> new ArrayList<>()).add(fix));
    }

    protected void addFix(@NotNull DataType.IdMapped type, @NotNull String id, @NotNull Function<Map<String, Object>, Map<String, Object>> fix) {
        ((DataTypeImpl.IdMapped) type).named(id)
                .fixes.computeIfAbsent(this.version, _ -> new ArrayList<>()).add(fix);
    }

}
