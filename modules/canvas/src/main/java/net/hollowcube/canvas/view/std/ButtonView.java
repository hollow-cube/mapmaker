package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A view with the given size, completely filled with the given item. Clicks into this view are
 * passed to the given {@link ClickHandler}.
 */
public class ButtonView implements View {
    private final int width, height;
    private final View view;
    private final ClickHandler clickHandler;

    public ButtonView(int width, int height, @NotNull ItemStack itemStack, @NotNull ClickHandler clickHandler) {
        this(new ItemView(width, height, itemStack), clickHandler);
    }

    public ButtonView(int width, int height, @NotNull ItemStack baseItem, @NotNull String key, @NotNull List<Component> args, @NotNull ClickHandler clickHandler) {
        this(new ItemView(width, height, baseItem, key, args), clickHandler);
    }

    public ButtonView(@NotNull View delegate, @NotNull ClickHandler clickHandler) {
        width = delegate.width();
        height = delegate.height();
        this.view = delegate;
        this.clickHandler = clickHandler;
    }

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
        return view.getContents();
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        return clickHandler.handleClick(player, slot, clickType);
    }

}
