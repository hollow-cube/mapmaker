package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.DataFix;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static net.hollowcube.datafix.util.DataFixUtils.namespaced;

public class BiomeRenameFix implements DataFix {
    private final Map<String, String> nameMap;

    public BiomeRenameFix(@NotNull String oldName, @NotNull String newName) {
        this.nameMap = Map.of(oldName, newName);
    }

    public BiomeRenameFix(@NotNull Map<String, String> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public Value fix(Value value) {
        if (!(value.value() instanceof String s)) return value;
        s = namespaced(s);
        return Value.wrap(nameMap.getOrDefault(s, s));
    }
}
