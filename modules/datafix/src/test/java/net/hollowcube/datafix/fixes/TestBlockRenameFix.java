package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestBlockRenameFix {

    @Test
    void blockName() {
        var fix = new BlockRenameFix("minecraft:grass", "minecraft:short_grass");
        var result = fix.fix(Value.wrap("minecraft:grass"));
        assertEquals("minecraft:short_grass", result.value());
    }

}
