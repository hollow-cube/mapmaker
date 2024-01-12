package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Cosmetic {

    public enum Rarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY
    }

    private final String type;
    private final String id;
    private final Rarity rarity;

    private final ItemStack icon;

    private Cosmetic(String type, String id, Rarity rarity) {
        this.type = type;
        this.id = id;
        this.rarity = rarity;

        this.icon = ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
                .displayName(LanguageProviderV2.translate(Component.translatable("cosmetic." + id + ".name")))
                .lore(LanguageProviderV2.translate(Component.translatable("cosmetic." + id + ".lore")))
                .meta(meta -> meta.customModelData(Objects.requireNonNull(BadSprite.SPRITE_MAP.get("cosmetic/" + id)).cmd()))
                .build();
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull Rarity rarity() {
        return rarity;
    }

    public @NotNull ItemStack icon() {
        return icon;
    }

    public static class Builder {
        private final String type;
        private final String id;
        private Rarity rarity = Rarity.COMMON;

        Builder(String type, String id) {
            this.type = type;
            this.id = id;
        }

        public @NotNull Builder rarity(@NotNull Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public @NotNull Cosmetic build() {
            return new Cosmetic(type, id, rarity);
        }
    }
}
