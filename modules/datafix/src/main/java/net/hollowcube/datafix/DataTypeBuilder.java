package net.hollowcube.datafix;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public final class DataTypeBuilder {
    final String id;

    final List<Property> properties = new ArrayList<>();
    // Keys here are is the encoded version (ie including subversion)
    final List<Pair<Integer, Function<Value, Value>>> fixes = new ArrayList<>();

    // ID mapped only
    final Map<String, DataTypeBuilder> idMap = new HashMap<>();

    public DataTypeBuilder(@NotNull String id) {
        this.id = id;
    }

    public void addProperty(@NotNull Property property) {
        for (var existing : properties) {
            if (Arrays.equals(property.path(), existing.path()) && property.getType().id() == existing.getType().id()) {
//                System.out.println("Skipping duplicate property " + property + " for " + id);
                return;
            }
        }
        properties.add(property);
    }

    public void addFix(int encodedVersion, @NotNull Function<Value, Value> fix) {
        fixes.add(Pair.of(encodedVersion, fix));
    }

    public @Nullable DataTypeBuilder get(@NotNull String id) {
        return this.idMap.get(id);
    }

    public @NotNull DataTypeBuilder getOrCreate(@NotNull String id) {
        return this.idMap.computeIfAbsent(id, DataTypeBuilder::new);
    }

}
