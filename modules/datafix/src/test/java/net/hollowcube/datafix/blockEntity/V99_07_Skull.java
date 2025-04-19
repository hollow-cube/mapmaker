package net.hollowcube.datafix.blockEntity;

import net.kyori.adventure.nbt.TagStringIOExt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class V99_07_Skull extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        var result = upgradeC(0, CURRENT);
        System.out.println(TagStringIOExt.writeTag(result, "    "));
        assertEquals("minecraft:skull", result.getString("id"));
    }

    @Test
    void profileMissingName() {
        // This is a difference from mojang which would insert an empty string here.
        // It should be acceptable difference however.
        var result = upgradeC(0, CURRENT);
        assertNull(result.getCompound("profile").get("name"));
    }

    @Test
    void upgradeProfileId() {
        var result = upgradeC(0, CURRENT);
        var id = result.getCompound("profile").getIntArray("id");
        assertEquals(4, id.length);
        assertEquals(-967193132, id[0]);
        assertEquals(-466204297, id[1]);
        assertEquals(-1498950319, id[2]);
        assertEquals(-1900097605, id[3]);
    }

    @Test
    void upgradeProfileProperties() {
        var result = upgradeC(0, CURRENT);
        var properties = result.getCompound("profile").getList("properties");
        assertEquals(1, properties.size());
        var prop = properties.getCompound(0);
        assertEquals("textures", prop.getString("name"));
        assertTrue(prop.getString("value").startsWith("eyJ0ZX"));
        assertNull(prop.get("signature"));
    }


}
