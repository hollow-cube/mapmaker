package net.hollowcube.datafix.entity;

import net.kyori.adventure.nbt.TagStringIOExt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_04_ArmorStand_1 extends AbstractEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void idUpgrade() {
        var result = upgradeC(0, CURRENT);
        System.out.println(TagStringIOExt.writeTag(result, "    "));
        assertEquals("minecraft:armor_stand", result.getString("id"));
    }

}
