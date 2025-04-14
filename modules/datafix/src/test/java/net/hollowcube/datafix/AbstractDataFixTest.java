package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDataFixTest {
    @BeforeAll
    public static void setup() {
        DataFixes.build();
    }

    protected Value upgradeFull(DataType dataType, Value value) {
        return upgrade(dataType, value, 0, Integer.MAX_VALUE);
    }

    protected Value upgrade(DataType dataType, Value value, int fromVersion, int toVersion) {
        var type = ((DataTypeImpl) dataType);
        var typeFixes = new ArrayList<>(type.fixes());

        if (type instanceof DataTypeIDMappedImpl idMapped) {
            var child = idMapped.get(value.get("id").as(String.class, ""));
            if (child != null) typeFixes.addAll(child.fixes());
        }

        for (var fix : typeFixes) {
            if (fix.first() <= fromVersion || fix.first() > toVersion) continue;
            var result = fix.value().apply(value);
            if (result != null) value = result;
        }

        return value;
    }

    protected Value wrap(@NotNull Map<String, Object> map) {
        return Value.wrap(new HashMap<>(map));
    }
}
