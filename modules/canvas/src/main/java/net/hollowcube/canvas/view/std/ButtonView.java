package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record ButtonView(
        int width, int height,
        @NotNull ItemStack itemStack,
        @NotNull ClickHandler clickHandler
) implements View {


    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public @NotNull ItemStack[] getContents() {
        var items = new ItemStack[width * height];
        Arrays.fill(items, itemStack);
        return items;
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        return clickHandler.handleClick(player, slot, clickType);
    }

}
