package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.hollowcube.canvas.section.ClickHandler;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class ItemLabelElement extends BaseItemElement implements Label, DepthAware, SpriteHolder, ItemSpriteHolder {
    private static final ItemStack BLANK_ITEM = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(1000))
            .build();

    private final String translationKey;
    private ItemStack itemSprite = BLANK_ITEM;

    public ItemLabelElement(@Nullable String id, int width, int height,
                            @NotNull String translationKey, @NotNull Component... args) {
        super(id, width, height);
        this.translationKey = translationKey;

        updateItem(args);
    }

    @Override
    public void setArgs(@NotNull Component... args) {
        updateItem(args);
    }

    @Override
    public void setItemSprite(@Nullable ItemStack itemStack) {
        itemSprite = itemStack == null ? BLANK_ITEM : itemStack;
        updateItem();
    }

    private void updateItem(@NotNull Component... args) {
        setItem(itemSprite.with(builder -> builder
                .displayName(Component.translatable(translationKey + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(translationKey + ".lore", List.of(args)))));
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        return ClickHandler.DENY;
    }

    @Override
    public BaseElement clone() {
        return new ItemLabelElement(id(), width(), height(), translationKey);
    }
}
