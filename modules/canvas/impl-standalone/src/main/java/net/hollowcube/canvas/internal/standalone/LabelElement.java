package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class LabelElement extends BaseElement implements Label, SpriteHolder, ItemSpriteHolder {
    private static final ItemStack BLANK_ITEM = NoxesiumAPI.setImmovable(ItemStack.builder(Material.STICK))
            .set(ItemComponent.CUSTOM_MODEL_DATA, System.getProperty("canvas.debug_blank", "0").equals("1") ? 2 : 1)
            .build();

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
        if (itemPosition != null) this.itemPosition = itemPosition;
        updateItem(List.of());
    }

    @Override
    public void setSprite(char fontChar, int cmd, int width, int offsetX, int rightOffset) {
        setSprite(new Sprite(fontChar, cmd, width, offsetX, rightOffset));
    }

    private void updateItem(@NotNull List<Component> args) {
        itemSprite = this.itemSprite.with(builder -> {
            builder.set(ItemComponent.CUSTOM_NAME, Component.translatable(translationKey + ".name", args));
            builder.set(ItemComponent.LORE, LanguageProviderV2.translateMulti(translationKey + ".lore", args));

            NoxesiumAPI.setImmovable(builder);
        });
        itemBlank = BLANK_ITEM.with(builder -> {
            builder.set(ItemComponent.CUSTOM_NAME, Component.translatable(translationKey + ".name", args));
            builder.set(ItemComponent.LORE, LanguageProviderV2.translateMulti(translationKey + ".lore", args));

            NoxesiumAPI.setImmovable(builder);
        });
        context.markDirty();
    }

    @Override
    public void setComponentsDirect(@Nullable Component title, @Nullable List<Component> lore) {
        itemSprite = this.itemSprite.with(builder -> {
            if (title != null) builder.set(ItemComponent.CUSTOM_NAME, title);
            if (lore != null) builder.set(ItemComponent.LORE, lore);

            NoxesiumAPI.setImmovable(builder);
        });
        itemBlank = BLANK_ITEM.with(builder -> {
            if (title != null) builder.set(ItemComponent.CUSTOM_NAME, title);
            if (lore != null) builder.set(ItemComponent.LORE, lore);

            NoxesiumAPI.setImmovable(builder);
        });
        context.markDirty();
    }

    @Override
    public void setItemDirect(@NotNull ItemStack itemStack) {
        itemSprite = NoxesiumAPI.setImmovable(itemStack);
        itemBlank = BLANK_ITEM.with(builder -> {
            builder.set(ItemComponent.CUSTOM_NAME, itemStack.get(ItemComponent.CUSTOM_NAME));
            builder.set(ItemComponent.LORE, itemStack.get(ItemComponent.LORE, List.of()));

            NoxesiumAPI.setImmovable(builder);
        });
        context.markDirty();
    }

    @Override
    public @NotNull ItemStack getItemDirect() {
        return itemSprite;
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new LabelElement(context, this);
    }
}
