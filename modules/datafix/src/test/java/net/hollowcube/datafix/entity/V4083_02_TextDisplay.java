package net.hollowcube.datafix.entity;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class V4083_02_TextDisplay extends AbstractEntityUpgradeTest {

    @Test
    void testUpgradePassengers() {
        var result = assertInstanceOf(CompoundBinaryTag.class, upgrade(4083, 4314));

        var text = assertInstanceOf(CompoundBinaryTag.class, result.get("text"));

        var extra2 = text.getList("extra").getCompound(2);
        assertEquals("✦", extra2.getString("text"));

    }
}
