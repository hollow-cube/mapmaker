package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractDataFixTest {
    @BeforeAll
    public static void setup() {
        DataFixer.buildModel();
    }

    protected Value upgradeFull(DataType dataType, Value value) {
        return upgrade(dataType, value, 0, Integer.MAX_VALUE);
    }

    protected Value upgrade(DataType dataType, Value value, int fromVersion, int toVersion) {
        fromVersion = Math.clamp(fromVersion, DataFixer.minVersion(), DataFixer.maxVersion());
        toVersion = Math.clamp(toVersion, DataFixer.minVersion(), DataFixer.maxVersion());
        if (fromVersion >= toVersion) return value;

        var typeSchema = DataFixer.schemas[dataType.id()];
        if (typeSchema == null) return value;

        // We can do a fancier fastpath for schemas with no children & no properties.
        if (typeSchema.oneshot()) return oneshotFastpath(typeSchema, value, fromVersion, toVersion);

        // Determine the initial schema to use, if this is an id mapped schema then the target may
        // be different.
        boolean isIdMapped = !typeSchema.idMap().isEmpty();
        String lastId = isIdMapped ? value.get("id").as(String.class, "") : null;
        var schema = isIdMapped ? typeSchema.idMap().getOrDefault(lastId, typeSchema) : typeSchema;

        for (int version = schema.relevantVersions().nextSetBit(fromVersion + 1);
             version >= 0 && version <= toVersion;
             version = schema.relevantVersions().nextSetBit(version + 1)
        ) {
            if (version == Integer.MAX_VALUE) break; // or (i+1) would overflow

//            System.out.println(version);

            int fixSpan = schema.versionToFixSpan().get(version);
            if (fixSpan != -1) {
                int startIndex = fixSpan >> 16, count = fixSpan & 0xFF;
                for (int i = 0; i < count; i++) {
                    var result = schema.fixes()[startIndex + i].fix(value);
                    if (result != null) value = result;
                }
            }

            // If this is an id mapped schema its possible that a fix has changed the ID of the schema,
            // in which case we need to find the current schema.
            if (isIdMapped) {
                var id = value.get("id").as(String.class, "");
                if (!Objects.equals(lastId, id)) {
                    lastId = id;
                    schema = typeSchema.idMap().getOrDefault(lastId, typeSchema);
                    if (schema == null) break; // no more fixes for this ID
                }
            }

            if (!schema.properties().isEmpty()) {
                int innerVersion = version - 1;
                for (var property : schema.properties()) {
                    forEachAtPath(value, property.path(), 0, v ->
                            upgrade(property.getType(), v, innerVersion, innerVersion + 1));
                }
            }
        }

        return value;
    }

    protected Value oneshotFastpath(OptimizedSchema schema, Value value, int fromVersion, int toVersion) {
        int firstRelevantVersion = schema.relevantVersions().nextSetBit(fromVersion + 1);
        int lastRelevantVersion = schema.relevantVersions().previousSetBit(toVersion);
        if (firstRelevantVersion == -1 || lastRelevantVersion == -1) return value;

        int startIndex = schema.versionToFixSpan().get(firstRelevantVersion) >> 16;
        int endIndex = schema.versionToFixSpan().get(lastRelevantVersion);
        endIndex = (endIndex >> 16) + (endIndex & 0xFF);

        var fixes = schema.fixes();
        for (int i = startIndex; i < endIndex; i++) {
            var result = fixes[i].fix(value);
            if (result != null) value = result;
        }

        return value;
    }

    protected Value forEachAtPath(Value parent, @NotNull String[] path, int i, DataFix fix) {
        if (i >= path.length) {
            // A small caveat of "extend"/zero path to modify the root object is its invalid
            // to replace the entire root object. For example a datafix for entity equipment may not
            // replace the entire entity.
            return fix.fix(parent);
        }

        var value = parent.get(path[i]);
        if (value.isNull()) return null;
        if (value.isListLike()) {
            for (int li = 0; li < value.size(0); li++) {
                var result = forEachAtPath(value.get(li), path, i + 1, fix);
                if (result != null) value.put(li, result);
            }
            return null;
        }

        var result = forEachAtPath(value, path, i + 1, fix);
        if (result != null) parent.put(path[i], result);
        return null;
    }

    protected Value wrap(@NotNull Map<String, Object> map) {
        return Value.wrap(new HashMap<>(map));
    }
}
