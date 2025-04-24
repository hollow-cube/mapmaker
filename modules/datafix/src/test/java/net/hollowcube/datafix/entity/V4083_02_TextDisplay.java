package net.hollowcube.datafix.entity;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIOExt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class V4083_02_TextDisplay extends AbstractEntityUpgradeTest {

    @Test
    void testUpgradePassengers() {
        var result = assertInstanceOf(CompoundBinaryTag.class, upgrade(4083, 4314));

        System.out.println(TagStringIOExt.writeTag(result, "    "));
        assertInstanceOf(CompoundBinaryTag.class, result.get("text"));
    }
}
