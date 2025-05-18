package net.hollowcube.mapmaker.panels;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Sprite(
        @NotNull String name,
        @NotNull BadSprite sprite,
        @Nullable BadSprite hoverSprite,
        int offsetX,
        int offsetY
) {

    public Sprite(@NotNull String sprite, int offsetX, int offsetY) {
        this(sprite, BadSprite.require(sprite), BadSprite.SPRITE_MAP.get(sprite + "_hover"), offsetX, offsetY);
    }

    public Sprite(@NotNull String sprite) {
        this(sprite, 0, 0);
    }

    public @NotNull Sprite withOffset(int offsetX, int offsetY) {
        return new Sprite(name, sprite, hoverSprite, offsetX, offsetY);
    }
}
