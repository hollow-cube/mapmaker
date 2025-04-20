package net.hollowcube.datafix.blockEntity;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.adventure.serializer.nbt.NbtComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_06_Sign extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:sign", result.getString("id"));
    }

    @Test
    void defaultWaxedValue() {
        var result = upgradeC(0, CURRENT);
        assertEquals(0, result.getByte("is_waxed"));
    }

    @Test
    void defaultBackText() {
        var result = upgradeC(0, CURRENT);
        var backText = result.getCompound("back_text");
        assertEquals(0, backText.getByte("has_glowing_text"));
        assertEquals("black", backText.getString("color"));
        var messages = backText.getList("messages");
        assertEquals(4, messages.size());
        for (var message : messages) {
            var component = assertDoesNotThrow(() -> NbtComponentSerializer.nbt().deserialize(message));
            assertEquals("", PlainTextComponentSerializer.plainText().serialize(component));
        }
    }

    @Test
    void frontText() {
        var result = upgradeC(0, CURRENT);
        var frontText = result.getCompound("front_text");
        assertEquals(0, frontText.getByte("has_glowing_text"));
        assertEquals("black", frontText.getString("color"));
        var messages = frontText.getList("messages");
        assertEquals(4, messages.size());
        for (int i = 0; i < 4; i++) {
            var message = messages.get(i);
            var component = assertDoesNotThrow(() -> NbtComponentSerializer.nbt().deserialize(message));
            assertEquals(switch (i) {
                case 1 -> "Ducati";
                case 2 -> "959 Panigale";
                default -> "";
            }, PlainTextComponentSerializer.plainText().serialize(component));
        }
    }

}
