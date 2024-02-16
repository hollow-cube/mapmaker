package net.hollowcube.map2.item.handler;

import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ItemUpdateFunc {
    void updateItemStack(ItemStack.@NotNull Builder builder, @NotNull TagHandler tag);
}
