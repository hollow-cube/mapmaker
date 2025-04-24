package net.hollowcube.datafix.entity;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class V4083_01_TextDisplay extends AbstractEntityUpgradeTest {

    @Test
    void testUpgradePassengers() {
        var result = assertInstanceOf(CompoundBinaryTag.class, upgrade(4083, 4314));

        var firstPassenger = assertInstanceOf(CompoundBinaryTag.class, result.getList("Passengers").get(1));
        assertInstanceOf(CompoundBinaryTag.class, firstPassenger.get("text"));
    }
}
