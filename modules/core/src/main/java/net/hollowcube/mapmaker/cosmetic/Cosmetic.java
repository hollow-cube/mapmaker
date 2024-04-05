package net.hollowcube.mapmaker.cosmetic;

import com.mojang.serialization.Codec;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.backpack.Rarity;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class Cosmetic {
    private static final Map<CosmeticType, Map<String, Cosmetic>> COSMETICS = new HashMap<>();

    public static final Codec<Cosmetic> CODEC = Codec.STRING.xmap(Cosmetic::byPathRequired, Cosmetic::path);

    static {
        try {
            Class.forName(Hats.class.getName());
            // Backwear
            Class.forName(Accessories.class.getName());
            // Pets
            // Emotes
            Class.forName(Particles.class.getName());
            Class.forName(VictoryEffects.class.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    public static @NotNull Comparator<Cosmetic> comparingName() {
        // May need to switch this to translate the name first, but for now this will do.
        return Comparator.comparing(c -> "cosmetic." + c.type.id() + "." + c.id + ".name");
    }

    public static <T> @NotNull Comparator<T> comparingName(Function<T, Cosmetic> getter) {
        // May need to switch this to translate the name first, but for now this will do.
        return Comparator.comparing(t -> {
            var c = getter.apply(t);
            return "cosmetic." + c.type.id() + "." + c.id + ".name";
        });
    }

    public static Comparator<Cosmetic> comparingRarity() {
        return Comparator.comparingInt(c -> c.rarity.ordinal());
    }

    public static <T> @NotNull Comparator<T> comparingRarity(@NotNull Function<T, Cosmetic> getter) {
        return Comparator.comparingInt(t -> getter.apply(t).rarity.ordinal());
    }

    private final CosmeticType type;
    private final String id;
    private final Rarity rarity;

    private final CosmeticImpl impl;

    private final ItemStack icon;
    private final ItemStack iconLocked;

    private Cosmetic(CosmeticType type, String id, Rarity rarity, Function<Cosmetic, CosmeticImpl> implFunc) {
        this.type = type;
        this.id = id;
        this.rarity = rarity;

        var displayName = displayName();
        var lore = lore();
        this.icon = ItemStack.of(Material.DIAMOND).withMeta(meta -> {
            meta.displayName(displayName);
            meta.lore(lore);
            var spritePath = "cosmetic/" + type.id() + "/" + id + "/icon";
            meta.customModelData(Objects.requireNonNull(BadSprite.SPRITE_MAP.get(spritePath), spritePath).cmd());
        });
        this.iconLocked = ItemStack.of(Material.DIAMOND).withMeta(meta -> {
            meta.displayName(displayName);
            meta.lore(lore);
            var spritePath = "cosmetic/" + type.id() + "/" + id + "/icon_locked";
            meta.customModelData(Objects.requireNonNull(BadSprite.SPRITE_MAP.get(spritePath), spritePath).cmd());
        });

        this.impl = implFunc.apply(this);
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

    public @NotNull ItemStack iconItem() {
        return icon;
    }

    public @NotNull ItemStack iconLockedItem() {
        return iconLocked;
    }

    public @NotNull CosmeticImpl impl() {
        return impl;
    }

    public @NotNull Component displayName() {
        return Objects.requireNonNull(LanguageProviderV2.translate(Component.translatable("cosmetic." + type.id() + "." + id + ".name")));
    }

    public @NotNull List<Component> lore() {
        var itemLore = new ArrayList<Component>();
        itemLore.add(rarity.asComponent());
        itemLore.add(Component.empty());
        itemLore.addAll(LanguageProviderV2.translateMulti("cosmetic." + type.id() + "." + id + ".lore", List.of()));
        return itemLore;
    }

    public static class Builder {
        private final CosmeticType type;
        private final String id;

        private Function<Cosmetic, CosmeticImpl> implFunc = CosmeticImpl::new;
        private Rarity rarity = Rarity.COMMON;

        Builder(CosmeticType type, String id) {
            this.type = type;
            this.id = id;
        }

        public @NotNull Builder impl(@NotNull Function<Cosmetic, CosmeticImpl> implFunc) {
            this.implFunc = implFunc;
            return this;
        }

        public @NotNull Builder rarity(@NotNull Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public @NotNull Cosmetic build() {
            var cosmetic = new Cosmetic(type, id, rarity, implFunc);
            COSMETICS.computeIfAbsent(type, k -> new HashMap<>()).put(id, cosmetic);
            return cosmetic;
        }
    }
}
