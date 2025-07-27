package net.hollowcube.mapmaker.map.item.handler;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Similar interface to {@link net.minestom.server.instance.block.BlockHandler}, but for items. Like block handlers,
 * implementations should be implemented as singletons, and registered to the appropriate {@link ItemRegistry} for
 * use within a {@link MapWorld}.
 * <p>
 * Items are identified by their {@link #key()}, which is hashed to set the custom model data of the item stack.
 */
public abstract class ItemHandler {

    public static final Tag<String> ID_TAG = Tag.String("mapmaker:handler");

    public static final int RIGHT_CLICK_AIR = 1 << 1;
    public static final int RIGHT_CLICK_BLOCK = 1 << 2;
    public static final int RIGHT_CLICK_ENTITY = 1 << 3;
    public static final int LEFT_CLICK_AIR = 1 << 4;
    public static final int LEFT_CLICK_BLOCK = 1 << 5;
    public static final int LEFT_CLICK_ENTITY = 1 << 6;

    // Not implemented very well, and not included in the ANY flags.
    public static final int LEFT_CLICK_GUI = 1 << 7;

    public static final int RIGHT_CLICK_ANY = RIGHT_CLICK_AIR | RIGHT_CLICK_BLOCK | RIGHT_CLICK_ENTITY;
    public static final int LEFT_CLICK_ANY = LEFT_CLICK_AIR | LEFT_CLICK_BLOCK | LEFT_CLICK_ENTITY;

    private final Key key;
    private final int flags;

    protected ItemHandler(@NotNull String key, int... flags) {
        this(Key.key(key), flags);
    }

    protected ItemHandler(@NotNull Key key, int... flags) {
        this.key = key;

        int flag = 0;
        for (int f : flags) flag |= f;
        this.flags = flag;
    }

    public @NotNull Key key() {
        return key;
    }

    /**
     * The sprite to represent this item.
     * <p>For more advanced item representation override {@link ItemHandler#build(ItemStack.Builder, CompoundBinaryTag)}</p>
     */
    public @Nullable BadSprite sprite() {
        return null;
    }

    public void build(@NotNull ItemStack.Builder builder, @Nullable CompoundBinaryTag tag) {
        var translation = String.format("item.%s.%s", key().namespace(), key().value());
        builder.set(DataComponents.CUSTOM_NAME, LanguageProviderV2.translate(Component.translatable(translation + ".name")));
        builder.set(DataComponents.LORE, LanguageProviderV2.translateMulti(translation + ".lore", List.of()));
        builder.setTag(ID_TAG, key().asString());
        builder.material(Material.STICK);

        var sprite = this.sprite();
        if (sprite != null) builder.set(DataComponents.ITEM_MODEL, sprite.model());
    }

    public final @NotNull ItemStack getItemStack() {
        return getItemStack(null);
    }

    public final @NotNull ItemStack getItemStack(@Nullable CompoundBinaryTag tag) {
        var builder = ItemStack.builder(Material.AIR);
        this.build(builder, tag);
        return builder.build();
    }

    protected void leftClicked(@NotNull Click click) {
    }

    protected void rightClicked(@NotNull Click click) {
    }

    protected final boolean allows(int flag) {
        return (flags & flag) != 0;
    }

    public record Click(
            @NotNull ItemHandler handler,
            @NotNull Player player,
            @NotNull ItemStack itemStack,
            @NotNull PlayerHand hand,
            @UnknownNullability Point blockPosition,
            @UnknownNullability Point placePosition,
            @UnknownNullability BlockFace face,
            @UnknownNullability Entity entity
    ) {

        public Click(
                @NotNull ItemHandler handler,
                @NotNull Player player,
                @NotNull ItemStack itemStack,
                @NotNull PlayerHand hand,
                @UnknownNullability Entity entity
        ) {
            this(handler, player, itemStack, hand, null, null, null, entity);
        }

        public @UnknownNullability Instance instance() {
            return player.getInstance();
        }

        public void consume(int amount) {
            this.update((stack, builder) -> builder.amount(stack.amount() - amount));
        }

        public void update(@NotNull BiConsumer<ItemStack, ItemStack.Builder> updater) {
            var stack = this.itemStack();
            this.player().setItemInHand(this.hand(), stack.with(builder -> {
                updater.accept(stack, builder);

                var sprite = this.handler().sprite();
                if (sprite != null) builder.set(DataComponents.ITEM_MODEL, sprite.model());
                builder.setTag(ID_TAG, this.handler().key().asString());
            }));
        }

        public void update(@NotNull Consumer<ItemStack.Builder> updater) {
            this.update((_, builder) -> updater.accept(builder));
        }
    }

}
