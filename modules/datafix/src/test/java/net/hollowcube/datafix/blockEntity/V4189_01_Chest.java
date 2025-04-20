package net.hollowcube.datafix.blockEntity;

import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.adventure.serializer.nbt.NbtComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class V4189_01_Chest extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeCustomNameFromString() {
        var result = upgradeC(4189, CURRENT);

        var customNameTag = result.getCompound("CustomName");
        var customName = assertDoesNotThrow(() -> NbtComponentSerializer.nbt().deserialize(customNameTag));
        assertEquals(NamedTextColor.GREEN, customName.color());
        assertEquals("Custom Chest", ((TextComponent) customName).content());
    }

    @Test
    void upgradeFlagStringToBool() {
        var result = upgradeC(4189, CURRENT);

        var customNameTag = result.getCompound("CustomName").get("bold");
        assertEquals(1, assertInstanceOf(ByteBinaryTag.class, customNameTag).value());
    }


}
