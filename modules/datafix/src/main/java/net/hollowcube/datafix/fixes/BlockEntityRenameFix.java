package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

import static net.hollowcube.datafix.util.DataFixUtils.namespaced;

public class BlockEntityRenameFix implements Function<Value, Value> {
    private final Map<String, String> nameMap;

    public BlockEntityRenameFix(@NotNull String oldName, @NotNull String newName) {
        this.nameMap = Map.of(oldName, newName);
    }

    public BlockEntityRenameFix(@NotNull Map<String, String> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public Value apply(Value value) {
        if (!(value.value() instanceof String s)) return value;
        s = namespaced(s);
        return Value.wrap(nameMap.getOrDefault(s, s));
    }
}
