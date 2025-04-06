package net.hollowcube.mapmaker.map.feature.play.effect;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public sealed interface HotbarItem {
    @SuppressWarnings({"RedundantCast", "unchecked"})
    @NotNull Codec<HotbarItem> CODEC = Codec.STRING.unionType(name -> (StructCodec<HotbarItem>) switch (name) {
        case Remove.ID -> Remove.CODEC;
        case FireworkRocket.ID -> FireworkRocket.CODEC;
        case Trident.ID -> Trident.CODEC;
        case null, default -> null;
    }, HotbarItem::name);

    @NotNull String name();

    @NotNull ItemStack toItemStack(boolean blank);

    default @NotNull HotbarItem fromItemStack(@NotNull ItemStack itemStack) {
        return this;
    }

    final class Remove implements HotbarItem {
        public static final Remove INSTANCE = new Remove();

        public static final String ID = "remove";
        public static final StructCodec<Remove> CODEC = StructCodec.struct(() -> INSTANCE);

        private static final ItemStack ITEM_STACK = ItemStack.builder(Material.DIAMOND)
                .set(DataComponents.ITEM_MODEL, BadSprite.require("effect/item/air").model())
                .customName(Component.translatable("gui.effect.item.remove.name"))
                .lore(Component.translatable("gui.effect.item.remove.lore"))
                .build();

        private Remove() {
        }

        @Override
        public @NotNull String name() {
            return ID;
        }

        @Override
        public @NotNull ItemStack toItemStack(boolean blank) {
            return ITEM_STACK;
        }

        @Override
        public String toString() {
            return ID;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    record FireworkRocket(int quantity, int duration) implements HotbarItem {
        public static final FireworkRocket DEFAULT = new FireworkRocket(0, 1_000);

        public static final String ID = "firework_rocket";
        public static final StructCodec<FireworkRocket> CODEC = StructCodec.struct(
                "quantity", ExtraCodecs.clamppedInt(1, 99).optional(1), FireworkRocket::quantity,
                "duration", Codec.INT.optional(1_000), FireworkRocket::duration,
                FireworkRocket::new);

        public @NotNull HotbarItem withQuantity(int amount) {
            return new FireworkRocket(amount, duration);
        }

        public @NotNull HotbarItem withDuration(int duration) {
            return new FireworkRocket(quantity, duration);
        }

        @Override
        public @NotNull String name() {
            return ID;
        }

        @Override
        public @NotNull ItemStack toItemStack(boolean blank) {
            return FireworkRocketItem.setDurationMillis(FireworkRocketItem.withCount(FireworkRocketItem.DEFAULT, quantity), duration);
        }

        @Override
        public @NotNull HotbarItem fromItemStack(@NotNull ItemStack itemStack) {
            if (itemStack.material() != Material.FIREWORK_ROCKET)
                return Remove.INSTANCE;

            return withQuantity(FireworkRocketItem.getCount(itemStack));
        }

        @Override
        public String toString() {
            return ID + "{quantity=" + quantity + ", duration=" + duration + "}";
        }
    }

    record Trident(int riptideLevel) implements HotbarItem {
        public static final Trident DEFAULT = new Trident(1);

        public static final String ID = "trident";
        public static final StructCodec<Trident> CODEC = StructCodec.struct(
                "riptideLevel", ExtraCodecs.clamppedInt(1, 3).optional(1), Trident::riptideLevel,
                Trident::new);

        private static final ItemStack DEFAULT_ITEM = ItemStack.of(Material.TRIDENT)
                .without(DataComponents.ATTRIBUTE_MODIFIERS);

        public HotbarItem withRiptideLevel(int level) {
            return new Trident(level);
        }

        @Override
        public @NotNull String name() {
            return ID;
        }

        @Override
        public @NotNull ItemStack toItemStack(boolean blank) {
            return blank ? DEFAULT_ITEM : DEFAULT_ITEM.with(DataComponents.ENCHANTMENTS,
                    EnchantmentList.EMPTY.with(Enchantment.RIPTIDE, riptideLevel));
        }

        @Override
        public String toString() {
            return ID + "{riptideLevel=" + riptideLevel + "}";
        }
    }
}
