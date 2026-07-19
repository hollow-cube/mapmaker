package net.hollowcube.mapmaker.editor.entity.editor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityEditorTest {

    static {
        MinecraftServer.init();
    }

    @Test
    void displaysNullPropertyValueAsNone() {
        assertEquals(
            Component.text("None", NamedTextColor.GRAY),
            EntityEditor.getValueDisplay(null)
        );
    }
}
