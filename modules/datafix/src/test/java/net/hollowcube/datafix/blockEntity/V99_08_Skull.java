package net.hollowcube.datafix.blockEntity;

import net.kyori.adventure.nbt.TagStringIOExt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Not super interesting compared to 07, just has no texture so should have no profile.
class V99_08_Skull extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        var result = upgradeC(0, CURRENT);
        System.out.println(TagStringIOExt.writeTag(result, "    "));
        assertEquals("minecraft:skull", result.getString("id"));
    }

    @Test
    void noProfileBecauseNoTexture() {
        var result = upgradeC(0, CURRENT);
        assertNull(result.get("profile"));
    }

}
