package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class LabelElement extends BaseElement implements Label, SpriteHolder, ItemSpriteHolder {
    private static final ItemStack BLANK_ITEM = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(System.getProperty("canvas.debug_blank", "0").equals("1") ? 2 : 1))
            .build();

    private static final ItemHideFlag[] ALL_HIDE_FLAGS = new ItemHideFlag[]{
            ItemHideFlag.HIDE_ENCHANTS, ItemHideFlag.HIDE_ATTRIBUTES, ItemHideFlag.HIDE_UNBREAKABLE,
            ItemHideFlag.HIDE_DESTROYS, ItemHideFlag.HIDE_PLACED_ON, ItemHideFlag.HIDE_POTION_EFFECTS,
            ItemHideFlag.HIDE_DYE
    };

    private final String translationKey;

    private ItemStack itemSprite = BLANK_ITEM;
    private ItemStack itemBlank = BLANK_ITEM;
    private Integer itemPosition = null;

    public LabelElement(@NotNull ElementContext context, @Nullable String id, int width, int height, @NotNull String translationKey) {
        super(context, id, width, height);
        this.translationKey = translationKey;
        updateItem(List.of());
    }

    protected LabelElement(@NotNull ElementContext context, @NotNull LabelElement other) {
        super(context, other);
        this.translationKey = other.translationKey;
        this.itemSprite = other.itemSprite;
        this.itemPosition = other.itemPosition;
        updateItem(List.of());
    }

    @Override
    public void setArgs(@NotNull Component... args) {
        updateItem(List.of(args));
    }

    @Override
    public void setArgs(@NotNull List<Component> args) {
        updateItem(args);
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        if (shouldDelegateDraw()) return super.getContents();

        var contents = new ItemStack[width() * height()];
        if (itemPosition != null) {
            Arrays.fill(contents, itemBlank);
            contents[itemPosition] = itemSprite;
        } else {
            Arrays.fill(contents, itemSprite);
        }
        return contents;
    }

    @Override
    public void setItemSprite(@Nullable ItemStack itemStack, @Nullable Integer itemPosition) {
        Check.argCondition(itemPosition != null && (itemPosition < 0 || itemPosition >= width() * height()),
                "Item position must be null or in range [0, " + (width() * height() - 1) + "], got " + itemPosition);

        this.itemSprite = itemStack == null ? BLANK_ITEM : itemStack;
        this.itemPosition = itemPosition;
        updateItem(List.of());
    }

    private void updateItem(@NotNull List<Component> args) {
        itemSprite = this.itemSprite.with(builder -> {
            builder.displayName(Component.translatable(translationKey + ".name", args));
            builder.lore(LanguageProviderV2.translateMulti(translationKey + ".lore", args));
            builder.meta(meta -> meta.hideFlag(ALL_HIDE_FLAGS));
        });
        itemBlank = BLANK_ITEM.with(builder -> {
            builder.displayName(Component.translatable(translationKey + ".name", args));
            builder.lore(LanguageProviderV2.translateMulti(translationKey + ".lore", args));
            builder.meta(meta -> meta.hideFlag(ALL_HIDE_FLAGS));
        });
        context.markDirty();
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new LabelElement(context, this);
    }
}
