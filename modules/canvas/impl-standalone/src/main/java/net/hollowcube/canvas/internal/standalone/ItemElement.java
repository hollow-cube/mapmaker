package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemElement extends LabelElement {

    private static final ItemStack BLANK_ITEM = ItemStack.builder(Material.STICK)
            .set(ItemComponent.ITEM_MODEL, "minecraft:air")
            .set(ItemComponent.HIDE_TOOLTIP)
            .build();

    public ItemElement(@NotNull ElementContext context, @Nullable String id, int width, int height) {
        super(context, id, width, height, "");
    }

    protected ItemElement(@NotNull ElementContext context, @NotNull ItemElement other) {
        super(context, other);
    }

    @Override
    public void setItemDirect(@NotNull ItemStack itemStack) {
        this.itemSprite = itemStack;
        this.updateItem();
    }

    @Override
    public void setComponentsDirect(@Nullable Component title, @Nullable List<Component> lore) {
        this.updateItem();
    }

    @Override
    protected void updateItem(@NotNull List<Component> args) {
        this.updateItem();
    }

    private void updateItem() {
        // It's an item element it doesnt have display components
        this.itemSprite = this.itemSprite.with(ItemComponent.HIDE_TOOLTIP);
        this.itemBlank = BLANK_ITEM;
        this.context.markDirty();
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new ItemElement(context, this);
    }
}
