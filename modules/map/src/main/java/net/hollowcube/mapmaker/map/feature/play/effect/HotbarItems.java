package net.hollowcube.mapmaker.map.feature.play.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

// Can store up to 3 items. If present, it will be merged (if similar) or  overwrite the existing item otherwise.
// Setting to air would clear the item
public record HotbarItems(
        @Nullable ItemStack item1,
        @Nullable ItemStack item2,
        @Nullable ItemStack item3,
        @Nullable Boolean elytra
) {
    public static final HotbarItems EMPTY = new HotbarItems(null, null, null, null);

    public static final Codec<HotbarItems> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.ITEM_STACK.optionalFieldOf("item1").forGetter(v -> Optional.ofNullable(v.item1())),
            ExtraCodecs.ITEM_STACK.optionalFieldOf("item2").forGetter(v -> Optional.ofNullable(v.item2())),
            ExtraCodecs.ITEM_STACK.optionalFieldOf("item3").forGetter(v -> Optional.ofNullable(v.item3())),
            Codec.BOOL.optionalFieldOf("elytra").forGetter(v -> Optional.ofNullable(v.elytra()))
    ).apply(i, HotbarItems::fromOptionals));

    private static @NotNull HotbarItems fromOptionals(
            @NotNull Optional<ItemStack> item1, @NotNull Optional<ItemStack> item2,
            @NotNull Optional<ItemStack> item3, @NotNull Optional<Boolean> elytra
    ) {
        return new HotbarItems(item1.orElse(null), item2.orElse(null), item3.orElse(null), elytra.orElse(null));
    }

    public @NotNull HotbarItems withItem(int index, @Nullable ItemStack item) {
        if (index == 0) {
            return new HotbarItems(item, item2, item3, elytra);
        } else if (index == 1) {
            return new HotbarItems(item1, item, item3, elytra);
        } else if (index == 2) {
            return new HotbarItems(item1, item2, item, elytra);
        } else {
            return this;
        }
    }

    public @NotNull HotbarItems withElytra(@Nullable Boolean elytra) {
        return new HotbarItems(item1, item2, item3, elytra);
    }

    @Override
    public String toString() {
        return "{" +
                "item1=" + (item1 == null ? "none" : item1.isAir() ? "remove" : item1) +
                ", item2=" + (item2 == null ? "none" : item2.isAir() ? "remove" : item2) +
                ", item3=" + (item3 == null ? "none" : item3.isAir() ? "remove" : item3) +
                ", elytra=" + (elytra == null ? "keep" : !elytra ? "remove" : "add") +
                '}';
    }

    public static class Mutable {
        private HotbarItems items;
        private Consumer<HotbarItems> onChange;

        public Mutable(@NotNull HotbarItems items, @Nullable Consumer<HotbarItems> onChange) {
            this.items = items;
            this.onChange = onChange;
        }

        public @Nullable ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> items.item1();
                case 1 -> items.item2();
                case 2 -> items.item3();
                default -> null;
            };
        }

        public void setItem(int index, @Nullable ItemStack item) {
            update(items.withItem(index, item));
        }

        public @Nullable Boolean getElytra() {
            return items.elytra();
        }

        public void setElytra(@Nullable Boolean elytra) {
            update(items.withElytra(elytra));
        }

        public void onChange(@NotNull Consumer<HotbarItems> onChange) {
            var old = this.onChange;
            this.onChange = (it) -> {
                onChange.accept(it);
                if (old != null) {
                    old.accept(it);
                }
            };
        }

        private void update(@NotNull HotbarItems items) {
            this.items = items;
            if (onChange != null) {
                onChange.accept(items);
            }
        }
    }
}
