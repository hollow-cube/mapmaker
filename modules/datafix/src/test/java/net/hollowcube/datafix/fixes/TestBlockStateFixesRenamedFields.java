package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestBlockStateFixesRenamedFields {

    @Test
    void blockRenameLegacyFields() {
        var blockState = Value.wrap(new HashMap<>(Map.of("Name", "minecraft:grass")));
        new BlockRenameFix("minecraft:grass", "minecraft:short_grass").fix(blockState);
        assertEquals("minecraft:short_grass", blockState.getValue("Name"));
        assertNull(blockState.getValue("id"));
    }

    @Test
    void blockRenameRenamedFields() {
        var blockState = Value.wrap(new HashMap<>(Map.of("id", "minecraft:grass")));
        new BlockRenameFix("minecraft:grass", "minecraft:short_grass").fix(blockState);
        assertEquals("minecraft:short_grass", blockState.getValue("id"));
        assertNull(blockState.getValue("Name"));
    }

    @Test
    void blockStatePropertiesLegacyFields() {
        var blockState = Value.wrap(new HashMap<>(Map.of(
                "Name", "minecraft:creaking_heart",
                "Properties", new HashMap<>(Map.of("active", "true")))));
        new BlockStatePropertiesFix("minecraft:creaking_heart", props -> props.put("active", "false")).fix(blockState);
        assertEquals("false", blockState.get("Properties").getValue("active"));
    }

    @Test
    void blockStatePropertiesRenamedFields() {
        var blockState = Value.wrap(new HashMap<>(Map.of(
                "id", "minecraft:creaking_heart",
                "properties", new HashMap<>(Map.of("active", "true")))));
        new BlockStatePropertiesFix("minecraft:creaking_heart", props -> props.put("active", "false")).fix(blockState);
        assertEquals("false", blockState.get("properties").getValue("active"));
    }

}
