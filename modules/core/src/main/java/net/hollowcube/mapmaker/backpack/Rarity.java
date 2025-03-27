package net.hollowcube.mapmaker.backpack;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public enum Rarity implements ComponentLike {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY;

    private final BadSprite iconSprite;

    Rarity() {
        var iconPath = "icon/rarity/" + name().toLowerCase();
        iconSprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get(iconPath), iconPath);
    }

    @Override
    public @NotNull Component asComponent() {
        return Component.text(iconSprite.fontChar(), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .shadowColor(ShadowColor.none());
    }
}
