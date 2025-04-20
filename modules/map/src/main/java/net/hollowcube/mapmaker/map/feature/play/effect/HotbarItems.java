package net.hollowcube.mapmaker.map.feature.play.effect;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

// Can store up to 3 items. If present, it will be merged (if similar) or  overwrite the existing item otherwise.
// Setting to air would clear the item
public record HotbarItems(
        @Nullable HotbarItem item1,
        @Nullable HotbarItem item2,
        @Nullable HotbarItem item3,
        @Nullable Boolean elytra
) {
    public static final HotbarItems EMPTY = new HotbarItems(null, null, null, null);

    public static final StructCodec<HotbarItems> CODEC = StructCodec.struct(
            "item1", HotbarItem.CODEC.optional(), HotbarItems::item1,
            "item2", HotbarItem.CODEC.optional(), HotbarItems::item2,
            "item3", HotbarItem.CODEC.optional(), HotbarItems::item3,
            "elytra", Codec.BOOLEAN.optional(), HotbarItems::elytra,
            HotbarItems::new);

    public @NotNull HotbarItems withItem(int index, @Nullable HotbarItem item) {
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
                "item1=" + (item1 == null ? "none" : item1) +
                ", item2=" + (item2 == null ? "none" : item2) +
                ", item3=" + (item3 == null ? "none" : item3) +
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

        public @Nullable HotbarItem getItem(int index) {
            return switch (index) {
                case 0 -> items.item1();
                case 1 -> items.item2();
                case 2 -> items.item3();
                default -> null;
            };
        }

        public void setItem(int index, @Nullable HotbarItem item) {
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
