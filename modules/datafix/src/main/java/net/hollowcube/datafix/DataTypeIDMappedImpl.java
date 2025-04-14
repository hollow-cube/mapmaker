package net.hollowcube.datafix;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

final class DataTypeIDMappedImpl extends DataTypeImpl implements DataType.IdMapped {
    private final Map<String, DataTypeImpl> idMap = new HashMap<>();

    public DataTypeIDMappedImpl(@NotNull Key key) {
        super(key);
    }

    public @Nullable DataTypeImpl get(@NotNull String id) {
        return this.idMap.get(id);
    }

    public @NotNull DataTypeImpl getOrCreate(@NotNull String id) {
        return this.idMap.computeIfAbsent(id, newId -> new DataTypeImpl(this, newId));
    }

    public void forEach(@NotNull BiConsumer<String, DataTypeImpl> consumer) {
        idMap.forEach(consumer);
    }
}
