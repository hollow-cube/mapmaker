package net.hollowcube.datafix.entity;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class V4997_01_PoplarChestBoat extends AbstractEntityUpgradeTest {

    @Test
    void testUpgradeChestBoatItems() {
        var result = upgradeC(4997, 5023);

        var item = assertInstanceOf(CompoundBinaryTag.class, result.getList("Items").get(0));
        assertEquals("minecraft:buried_trial_chambers_map", item.getString("id"));
    }
}
