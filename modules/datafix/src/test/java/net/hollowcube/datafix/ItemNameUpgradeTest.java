package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemNameUpgradeTest extends AbstractDataFixTest {

    @ValueSource(strings = {
            "minecraft:speckled_melon->minecraft:glistering_melon_slice",
            "minecraft:spawn_egg->minecraft:spawn_egg", // This is invalid but needs the itemstack context to be correct
            "minecraft:cooked_fished->minecraft:cooked_fish",
    })
    @ParameterizedTest
    void testV99ToV4314(String input) {
        var split = input.split("->");
        assertEquals(2, split.length);
        var result = upgrade(DataTypes.ITEM_NAME, Value.wrap(split[0]), 0, 4314);
        assertEquals(split[1], result.value());
    }

}
