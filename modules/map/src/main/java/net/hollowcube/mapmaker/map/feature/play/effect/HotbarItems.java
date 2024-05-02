package net.hollowcube.mapmaker.map.feature.play.effect;

import com.mojang.serialization.Codec;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// Can store up to 3 items. If present, it will be merged (if similar) /overwrite the existing item.
// Setting to air would clear the item
public record HotbarItems(
        @Nullable ItemStack item1,
        @Nullable ItemStack item2,
        @Nullable ItemStack item3
) {
    public static final HotbarItems EMPTY = new HotbarItems(null, null, null);

    public static final Codec<HotbarItems> CODEC = Codec.unit(EMPTY);

//    public static final Codec<HotbarItems> CODEC = RecordCodecBuilder.create(i -> i.group(
//            ItemStack.CODEC.optionalFieldOf("item1").forGetter(v -> Optional.ofNullable(v.item1())),
//            ItemStack.CODEC.optionalFieldOf("item2").forGetter(v -> Optional.ofNullable(v.item2())),
//            ItemStack.CODEC.optionalFieldOf("item3").forGetter(v -> Optional.ofNullable(v.item3()))
//    ).apply(i, HotbarItems::new));

//    public HotbarItems(@NotNull Optional<ItemStack> item1, @NotNull Optional<ItemStack> item2, @NotNull Optional<ItemStack> item3) {
//        this(item1.orElse(null), item2.orElse(null), item3.orElse(null));
//    }

}
