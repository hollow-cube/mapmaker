package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class BlockRenameFix implements Function<Value, Value> {
    private final Map<String, String> nameMap;

    public BlockRenameFix(@NotNull String oldName, @NotNull String newName) {
        this.nameMap = Map.of(oldName, newName);
    }

    public BlockRenameFix(@NotNull Map<String, String> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public Value apply(Value value) {
        throw new UnsupportedOperationException("todo");
    }
}
