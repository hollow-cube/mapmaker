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
        if (value.value() instanceof String s) { // FLAT_BLOCK_STATE or BLOCK_NAME
            int index = s.indexOf('[');
            if (index == -1) return Value.wrap(nameMap.getOrDefault(s, s));
            var blockName = s.substring(0, index);
            return Value.wrap(nameMap.getOrDefault(blockName, blockName) + s.substring(index));
        } else if (value.isMapLike()) { // BLOCK_STATE
            var oldName = value.get("Name").as(String.class, "");
            value.put("Name", Value.wrap(nameMap.getOrDefault(oldName, oldName)));
            return null;
        }
        return null;
    }
}
