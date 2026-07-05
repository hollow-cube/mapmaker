package net.hollowcube.datafix.entity;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class V4888_01_Nameplate extends AbstractEntityUpgradeTest {

    @Test
    void testRenameNameplateDistance() {
        var result = upgradeC(4763, 4888);

        var attributes = result.getList("attributes");
        var first = assertInstanceOf(CompoundBinaryTag.class, attributes.get(0));
        var second = assertInstanceOf(CompoundBinaryTag.class, attributes.get(1));
        assertEquals("minecraft:name_tag_distance", first.getString("id"));
        assertEquals("minecraft:max_health", second.getString("id"));
    }
}
