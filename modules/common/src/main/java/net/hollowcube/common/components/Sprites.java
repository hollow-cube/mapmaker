package net.hollowcube.common.components;

import net.hollowcube.common.util.font.FontSpacing;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import org.jetbrains.annotations.Nullable;

/// Sprite object components the server resolves into font glyphs before they are sent, so that
/// code without access to the font (ipc types, for one) can still name a sprite: `mapmaker:sprites`
/// is a [BadSprite] by name, `mapmaker:space` a horizontal offset in pixels.
public final class Sprites {
    public static final Key ATLAS = Key.key("mapmaker", "sprites");
    public static final Key SPACE_ATLAS = Key.key("mapmaker", "space");

    public static Component sprite(String name) {
        return Component.object(ObjectContents.sprite(ATLAS, Key.key("mapmaker", name)));
    }

    public static Component space(int pixels) {
        return Component.object(ObjectContents.sprite(SPACE_ATLAS, Key.key("mapmaker", String.valueOf(pixels))));
    }

    /// The font text `object` stands for, or null when it is not one of ours.
    public static @Nullable String text(ObjectComponent object) {
        if (!(object.contents() instanceof SpriteObjectContents sprite)) return null;
        if (sprite.atlas().equals(ATLAS)) return String.valueOf(BadSprite.require(sprite.sprite().value()).fontChar());
        if (sprite.atlas().equals(SPACE_ATLAS)) return FontSpacing.compute(Integer.parseInt(sprite.sprite().value()));
        return null;
    }

    /// `object` as the text it will be sent as, keeping its style and children, or null when it is
    /// not one of ours.
    public static @Nullable Component resolve(ObjectComponent object) {
        var text = text(object);
        return text == null ? null : Component.text(text, object.style()).children(object.children());
    }

    private Sprites() {}
}
