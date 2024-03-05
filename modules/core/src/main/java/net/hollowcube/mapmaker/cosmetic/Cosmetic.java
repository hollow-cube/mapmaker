package net.hollowcube.mapmaker.cosmetic;

import com.mojang.serialization.Codec;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.backpack.Rarity;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.color.Color;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Cosmetic {
    private static final Map<CosmeticType, Map<String, Cosmetic>> COSMETICS = new HashMap<>();

    public static final Codec<Cosmetic> CODEC = Codec.STRING.xmap(Cosmetic::byPathRequired, Cosmetic::path);

    static {
        // Class init required
        var ignored = HeadCosmetics.CROWN;
    }

    // In the form type/id
    public static @NotNull Cosmetic byPathRequired(@NotNull String path) {
        return Objects.requireNonNull(byPath(path), "Cosmetic not found: " + path);
    }

    // In the form type/id
    public static @Nullable Cosmetic byPath(@NotNull String path) {
        String[] split = path.split("/");
        if (split.length != 2) return null;
        return byId(CosmeticType.valueOf(split[0].toUpperCase(Locale.ROOT)), split[1]);
    }

    public static @Nullable Cosmetic byId(@NotNull CosmeticType type, @Nullable String id) {
        if (id == null) return null;
        var byType = COSMETICS.get(type);
        if (byType == null) return null;
        return byType.get(id);
    }

    public static @NotNull Collection<Cosmetic> values(@NotNull CosmeticType type) {
        return COSMETICS.getOrDefault(type, Map.of()).values();
    }

    public static @NotNull Comparator<Cosmetic> comparingRarity() {
        return Comparator.comparingInt(c -> c.rarity.ordinal());
    }

    private final CosmeticType type;
    private final String id;
    private final Rarity rarity;

    private final ItemStack icon;

    private Cosmetic(CosmeticType type, String id, Rarity rarity) {
        this.type = type;
        this.id = id;
        this.rarity = rarity;

        this.icon = ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
                .displayName(LanguageProviderV2.translate(Component.translatable("cosmetic." + type.id() + "." + id + ".name")))
                .lore(LanguageProviderV2.translateMulti("cosmetic." + type.id() + ".lore",
                        List.of(rarity().asComponent(), Component.translatable("cosmetic." + type.id() + "." + id + ".lore"))))
                .meta(LeatherArmorMeta.class, meta -> {
                    var spritePath = "models/cosmetics/" + type.id() + "/" + id;
                    meta.customModelData(Objects.requireNonNull(BadSprite.SPRITE_MAP.get(spritePath), spritePath).cmd());
                    meta.color(new Color(255, 255, 255));
                    meta.hideFlag(ItemHideFlag.HIDE_DYE);
                })
                .build();
    }

    public @NotNull CosmeticType type() {
        return type;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String path() {
        return type.id() + "/" + id;
    }

    public @NotNull Rarity rarity() {
        return rarity;
    }

    public @NotNull ItemStack icon() {
        return icon;
    }

    public static class Builder {
        private final CosmeticType type;
        private final String id;
        private Rarity rarity = Rarity.COMMON;

        Builder(CosmeticType type, String id) {
            this.type = type;
            this.id = id;
        }

        public @NotNull Builder rarity(@NotNull Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public @NotNull Cosmetic build() {
            var cosmetic = new Cosmetic(type, id, rarity);
            COSMETICS.computeIfAbsent(type, k -> new HashMap<>()).put(id, cosmetic);
            return cosmetic;
        }
    }
}
