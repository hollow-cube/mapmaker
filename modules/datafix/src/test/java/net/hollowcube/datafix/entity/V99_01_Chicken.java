package net.hollowcube.datafix.entity;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class V99_01_Chicken extends AbstractEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void idUpgrade() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:chicken", result.getString("id"));
    }

    @Test
    void healFToHealth() {
        var result = upgradeC(0, CURRENT);
        assertEquals(4f, result.getFloat("Health"));
    }

    @Test
    void uuidToInline() {
        var result = upgradeC(0, CURRENT);
        // Yes, this is inverted. See related comment in UUIDFixes.
        var uuid = result.getIntArray("UUID");
        assertEquals(4, uuid.length);
        assertEquals(558221653, uuid[0]);
        assertEquals(1914389031, uuid[1]);
        assertEquals(-1966457728, uuid[2]);
        assertEquals(-1680752328, uuid[3]);
    }

    @Test
    void emptyFieldsSkipped() {
        var result = upgradeC(0, CURRENT);
        var keys = result.keySet();

        assertFalse(keys.contains("equipment"), "equipment");
        assertFalse(keys.contains("CustomName"), "CustomName");
        assertFalse(keys.contains("LoveCause"), "LoveCause");
        assertFalse(keys.contains("HealF"), "HealF");
        assertFalse(keys.contains("drop_chances"), "drop_chances");

        for (var attributeTag : result.getList("attributes", BinaryTagTypes.COMPOUND)) {
            var attribute = (CompoundBinaryTag) attributeTag;
            var id = attribute.getString("id");

            if (!"minecraft:follow_range".equals(id))
                assertFalse(attribute.keySet().contains("modifiers"), id);
        }
    }

    @Test
    void attributeModifierObjectFormat() {
        var result = upgradeC(0, CURRENT);
        // Yes, this is inverted. See related comment in UUIDFixes.
        var randomSpawnBonus = result.getList("attributes").getCompound(3)
                .getList("modifiers").getCompound(0);
        assertEquals("minecraft:random_spawn_bonus", randomSpawnBonus.getString("id"));
        assertEquals("add_multiplied_base", randomSpawnBonus.getString("operation"));
        assertEquals(-0.016318803203646825, randomSpawnBonus.getDouble("amount"));
    }

}
