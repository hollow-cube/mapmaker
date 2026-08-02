package net.hollowcube.mapmaker.panels;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.Nullable;

public record Sprite(
    String name,
    BadSprite sprite,
    /// Hover art drawn for this sprite specifically, covering the whole button.
    @Nullable BadSprite hoverSprite,
    /// The generated border of this sprite, only meaningful when it is a button's background.
    @Nullable BadSprite outlineSprite,
    int offsetX,
    int offsetY
) {

    public Sprite(String sprite, int offsetX, int offsetY) {
        this(sprite, BadSprite.require(sprite), BadSprite.SPRITE_MAP.get(sprite + "_hover"),
            BadSprite.SPRITE_MAP.get(sprite + "_outline"), offsetX, offsetY);
    }

    public Sprite(String sprite) {
        this(sprite, 0, 0);
    }

    public Sprite withOffset(int offsetX, int offsetY) {
        return new Sprite(name, sprite, hoverSprite, outlineSprite, offsetX, offsetY);
    }
}
