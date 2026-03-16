package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.DataFix;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

public class BlockRenameFix implements DataFix {
    private final Map<String, String> nameMap;

    public BlockRenameFix(String oldName, String newName) {
        this.nameMap = Map.of(oldName, newName);
    }

    public BlockRenameFix(Map<String, String> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public Value fix(Value value) {
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
