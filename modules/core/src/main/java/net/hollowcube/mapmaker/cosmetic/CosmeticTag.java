package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public enum CosmeticTag implements ComponentLike {
    LIMITED_TIME;

    private final BadSprite iconSprite;

    CosmeticTag() {
        var iconPath = "icon/cosmetic_tag/" + name().toLowerCase();
        iconSprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get(iconPath), iconPath);
    }

    @Override
    public @NotNull Component asComponent() {
        return Component.text(iconSprite.fontChar(), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .shadowColor(ShadowColor.none());
    }
}
