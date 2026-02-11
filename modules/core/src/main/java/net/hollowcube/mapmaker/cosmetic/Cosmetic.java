package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.backpack.Rarity;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.hollowcube.mapmaker.cosmetic.impl.ModelCosmeticImpl;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minestom.server.codec.Codec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class Cosmetic {
    private static final CustomModelData LOCKED_CMD = new CustomModelData(List.of(1f), List.of(), List.of(), List.of());
    private static final Map<CosmeticType, Map<String, Cosmetic>> COSMETICS = new HashMap<>();

    public static final Codec<Cosmetic> CODEC = Codec.STRING.transform(Cosmetic::byPathRequired, Cosmetic::path);
    public static final Tag<Boolean> COSMETIC_TAG = Tag.Boolean("cosmetic");

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
    private final boolean hidden;
    private final Set<CosmeticTag> tags;

    private final CosmeticImpl impl;

    private final ItemStack icon;
    private final ItemStack iconLocked;

    private Cosmetic(Builder builder) {
        this.type = builder.type;
        this.id = builder.id;
        this.rarity = builder.rarity;
        this.hidden = builder.hidden;
        this.tags = Set.copyOf(builder.tags);

        var displayName = displayName();
        var lore = lore();
        this.icon = ItemStack.builder(Material.DIAMOND)
                .set(DataComponents.CUSTOM_NAME, displayName)
                .set(DataComponents.LORE, lore)
                .set(DataComponents.ITEM_MODEL, BadSprite.require("cosmetic/" + type.id() + "/" + id).model())
                .build().withTag(COSMETIC_TAG, true);
        this.iconLocked = icon.with(DataComponents.CUSTOM_MODEL_DATA, LOCKED_CMD);

        this.impl = builder.implFunc.apply(this);
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

    public boolean isHidden() {
        return hidden;
    }

    public @NotNull ItemStack iconItem() {
        return icon;
    }

    public @NotNull ItemStack iconLockedItem() {
        return iconLocked;
    }

    public @NotNull ItemStack iconPreviewItem() {
        return icon.builder()
            .customModelData(List.of(2f), List.of(), List.of(), List.of())
            .build();
    }

    public @NotNull CosmeticImpl impl() {
        return impl;
    }

    public boolean canBePreviewed() {
        return impl instanceof ModelCosmeticImpl;
    }

    public @NotNull Component displayName() {
        return Objects.requireNonNull(LanguageProviderV2.translate(Component.translatable("cosmetic." + type.id() + "." + id + ".name")));
    }

    public @NotNull List<Component> lore() {
        var itemLore = new ArrayList<Component>();
        itemLore.add(rarity.asComponent());
        if (!tags.isEmpty()) itemLore.add(Component.join(JoinConfiguration.noSeparators(), tags));
        itemLore.add(Component.empty());
        itemLore.addAll(LanguageProviderV2.translateMulti("cosmetic." + type.id() + "." + id + ".lore", List.of()));
        return itemLore;
    }

    public static class Builder {
        private final CosmeticType type;
        private final String id;
        private final Set<CosmeticTag> tags = new HashSet<>();

        private Function<Cosmetic, CosmeticImpl> implFunc = CosmeticImpl::new;
        private Rarity rarity = Rarity.COMMON;
        private boolean hidden = false;

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

        public @NotNull Builder hidden() {
            this.hidden = true;
            return this;
        }

        public @NotNull Builder tags(@NotNull CosmeticTag... tags) {
            this.tags.addAll(Arrays.asList(tags));
            return this;
        }

        public @NotNull Cosmetic build() {
            var cosmetic = new Cosmetic(this);
            COSMETICS.computeIfAbsent(type, k -> new HashMap<>()).put(id, cosmetic);
            return cosmetic;
        }
    }
}
