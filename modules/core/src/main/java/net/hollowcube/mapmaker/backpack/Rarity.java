package net.hollowcube.mapmaker.backpack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

public enum Rarity implements ComponentLike {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY;

    @Override
    public @NotNull Component asComponent() {
        return Component.translatable("cosmetic.rarity." + name().toLowerCase());
    }
}
