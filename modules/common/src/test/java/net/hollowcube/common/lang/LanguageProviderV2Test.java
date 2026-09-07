package net.hollowcube.common.lang;

import net.hollowcube.common.components.Sprites;
import net.hollowcube.common.util.font.FontSpacing;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.object.ObjectContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LanguageProviderV2Test {

    @Test
    void spaceSpriteResolvesToFontOffsetKeepingStyleAndChildren() {
        var sprite = Sprites.space(3).color(NamedTextColor.RED).append(Component.text("x"));

        var resolved = assertInstanceOf(TextComponent.class, LanguageProviderV2.translate(sprite));
        assertEquals(FontSpacing.compute(3), resolved.content());
        assertEquals(NamedTextColor.RED, resolved.color());
        assertEquals(Component.text("x"), resolved.children().getFirst());
    }

    @Test
    void foreignSpritePassesThrough() {
        var sprite = Component.object(ObjectContents.sprite(Key.key("minecraft", "blocks"), Key.key("minecraft", "stone")));
        assertEquals(sprite, LanguageProviderV2.translate(sprite));
    }

    @Test
    void hoverTextIsTranslatedToo() {
        var text = Component.text("hi").hoverEvent(HoverEvent.showText(Sprites.space(2)));

        var hover = LanguageProviderV2.translate(text).hoverEvent();
        assertEquals(Component.text(FontSpacing.compute(2)), hover.value());
    }
}
