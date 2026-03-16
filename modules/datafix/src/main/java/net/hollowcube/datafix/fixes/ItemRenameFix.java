package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.DataFix;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

import static net.hollowcube.datafix.util.DataFixUtils.namespaced;

public class ItemRenameFix implements DataFix {
    private final Map<String, String> nameMap;

    public ItemRenameFix(String oldName, String newName) {
        this.nameMap = Map.of(oldName, newName);
    }

    public ItemRenameFix(Map<String, String> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public Value fix(Value value) {
        if (!(value.value() instanceof String s)) return value;
        s = namespaced(s);
        return Value.wrap(nameMap.getOrDefault(s, s));
    }
}
