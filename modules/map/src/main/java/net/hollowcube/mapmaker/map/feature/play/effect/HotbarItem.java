package net.hollowcube.mapmaker.map.feature.play.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import org.jetbrains.annotations.NotNull;

public sealed interface HotbarItem {
    @NotNull Codec<HotbarItem> CODEC = Codec.STRING.dispatch(HotbarItem::name, name -> switch (name) {
        case Remove.ID -> Remove.CODEC;
        case FireworkRocket.ID -> FireworkRocket.CODEC;
        case Trident.ID -> Trident.CODEC;
        case null, default -> null;
    });

    @NotNull String name();

    @NotNull ItemStack toItemStack(boolean blank);

    default @NotNull HotbarItem fromItemStack(@NotNull ItemStack itemStack) {
        return this;
    }

    final class Remove implements HotbarItem {
        public static final Remove INSTANCE = new Remove();

        public static final String ID = "remove";
        public static final MapCodec<Remove> CODEC = MapCodec.unit(INSTANCE);

        private static final ItemStack ITEM_STACK = ItemStack.builder(Material.DIAMOND)
                .set(ItemComponent.CUSTOM_MODEL_DATA, BadSprite.require("effect/item/air").cmd())
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

    record FireworkRocket(int quantity, int duration) implements HotbarItem {
        public static final FireworkRocket DEFAULT = new FireworkRocket(0, 1_000);

        public static final String ID = "firework_rocket";
        public static final MapCodec<FireworkRocket> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("quantity").forGetter(FireworkRocket::quantity),
                Codec.INT.fieldOf("duration").forGetter(FireworkRocket::duration)
        ).apply(i, FireworkRocket::new));

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
        public static final MapCodec<Trident> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("riptideLevel").forGetter(Trident::riptideLevel)
        ).apply(i, Trident::new));

        private static final ItemStack DEFAULT_ITEM = ItemStack.of(Material.TRIDENT)
                .without(ItemComponent.ATTRIBUTE_MODIFIERS);

        public HotbarItem withRiptideLevel(int level) {
            return new Trident(level);
        }

        @Override
        public @NotNull String name() {
            return ID;
        }

        @Override
        public @NotNull ItemStack toItemStack(boolean blank) {
            return blank ? DEFAULT_ITEM : DEFAULT_ITEM.with(ItemComponent.ENCHANTMENTS,
                    EnchantmentList.EMPTY.with(Enchantment.RIPTIDE, riptideLevel));
        }

        @Override
        public String toString() {
            return ID + "{riptideLevel=" + riptideLevel + "}";
        }
    }
}
