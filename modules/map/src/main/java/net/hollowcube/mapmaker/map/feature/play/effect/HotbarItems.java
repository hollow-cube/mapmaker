package net.hollowcube.mapmaker.map.feature.play.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.util.dfu.ExtraCodecs;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

// Can store up to 3 items. If present, it will be merged (if similar) or  overwrite the existing item otherwise.
// Setting to air would clear the item
public record HotbarItems(
        @Nullable ItemStack item1,
        @Nullable ItemStack item2,
        @Nullable ItemStack item3
) {
    public static final HotbarItems EMPTY = new HotbarItems(null, null, null);

    public static final Codec<HotbarItems> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.ITEM_STACK.optionalFieldOf("item1").forGetter(v -> Optional.ofNullable(v.item1())),
            ExtraCodecs.ITEM_STACK.optionalFieldOf("item2").forGetter(v -> Optional.ofNullable(v.item2())),
            ExtraCodecs.ITEM_STACK.optionalFieldOf("item3").forGetter(v -> Optional.ofNullable(v.item3()))
    ).apply(i, HotbarItems::fromOptionals));

    private static @NotNull HotbarItems fromOptionals(@NotNull Optional<ItemStack> item1, @NotNull Optional<ItemStack> item2, @NotNull Optional<ItemStack> item3) {
        return new HotbarItems(item1.orElse(null), item2.orElse(null), item3.orElse(null));
    }

    @Override
    public String toString() {
        return "{" +
                "item1=" + (item1 == null ? "none" : item1.isAir() ? "remove" : item1) +
                ", item2=" + (item2 == null ? "none" : item2.isAir() ? "remove" : item2) +
                ", item3=" + (item3 == null ? "none" : item3.isAir() ? "remove" : item3) +
                '}';
    }
}
