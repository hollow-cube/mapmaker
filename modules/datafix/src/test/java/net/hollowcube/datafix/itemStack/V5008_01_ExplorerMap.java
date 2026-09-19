package net.hollowcube.datafix.itemStack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V5008_01_ExplorerMap extends AbstractItemStackUpgradeTest {

    @Test
    void testExplorerMapItem() {
        var result = upgradeC(4903, 5008);

        assertEquals("minecraft:woodland_explorer_map", result.getString("id"));
        var components = result.getCompound("components");
        assertFalse(components.keySet().contains("minecraft:item_name"));
        assertFalse(components.keySet().contains("minecraft:map_color"));
        assertEquals(3, components.getInt("minecraft:map_id"));
        assertTrue(components.getCompound("minecraft:map_decorations").keySet().contains("+"));
    }

    @Test
    void testExplorerMapRename() {
        var result = upgradeC(4903, 5023);
        assertEquals("minecraft:woodland_mansion_map", result.getString("id"));
    }
}
