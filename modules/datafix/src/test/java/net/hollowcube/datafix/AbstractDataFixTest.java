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
        DataFixes.build();
    }

    protected Value upgradeFull(DataType dataType, Value value) {
        return upgrade(dataType, value, 0, Integer.MAX_VALUE);
    }

    protected Value upgrade(DataType dataType, Value value, int fromVersion, int toVersion) {
        fromVersion = Math.clamp(fromVersion, DataFixes.minVersion(), DataFixes.maxVersion());
        toVersion = Math.clamp(toVersion, DataFixes.minVersion(), DataFixes.maxVersion());
        if (fromVersion >= toVersion) return value;

        String oldId = null;
        var type = ((DataTypeImpl) dataType);
        if (type instanceof DataTypeIDMappedImpl idMapped) {
            oldId = value.getValue("id") instanceof String s ? s : "";
            type = Objects.requireNonNullElse(idMapped.get(oldId), idMapped);
        }

        var fixBits = type.relevantVersions;
        for (int version = fixBits.nextSetBit(fromVersion + 1);
             version >= 0 && version <= toVersion;
             version = fixBits.nextSetBit(version + 1)
        ) {
            if (version == Integer.MAX_VALUE) break; // or (i+1) would overflow
//            System.out.println(version + " " + oldId);

            for (var fix : type.fixes()) {
                if (fix.first() != version) continue;

                var result = fix.value().apply(value);
                if (result != null) value = result;

                var newId = value.getValue("id") instanceof String s ? s : null;
                if (!Objects.equals(oldId, newId) && dataType instanceof DataTypeIDMappedImpl idMapped) {
                    oldId = newId;
                    type = Objects.requireNonNullElse(idMapped.get(Objects.requireNonNullElse(newId, "")), idMapped);
                    fixBits = type.relevantVersions;
                }
            }
        }

        return value;
    }

    protected Value wrap(@NotNull Map<String, Object> map) {
        return Value.wrap(new HashMap<>(map));
    }
}
