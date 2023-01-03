package net.hollowcube.canvas.view;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.std.ButtonView;
import net.hollowcube.canvas.view.std.ItemView;
import net.hollowcube.canvas.view.std.PaneView;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.NotNull;

public interface View {

    // Pane/group

    @Pure
    static @NotNull ParentView Pane(int width, int height) {
        return new PaneView(width, height);
    }

    // Single item

    @Pure
    static @NotNull View Item(@NotNull ItemStack itemStack) {
        return new ItemView(itemStack);
    }

    // Button

    @Pure
    static @NotNull View Button(@NotNull ItemStack itemStack, @NotNull Runnable onClick) {
        return Button(1, 1, itemStack, ClickHandler.leftClick(onClick));
    }

    @Pure
    static @NotNull View Button(@NotNull ItemStack itemStack, @NotNull ClickHandler clickHandler) {
        return Button(1, 1, itemStack, clickHandler);
    }

    @Pure
    static @NotNull View Button(int width, int height, @NotNull ItemStack itemStack, @NotNull Runnable onClick) {
        return Button(width, height, itemStack, ClickHandler.leftClick(onClick));
    }

    @Pure
    static @NotNull View Button(int width, int height, @NotNull ItemStack itemStack, @NotNull ClickHandler clickHandler) {
        return new ButtonView(width, height, itemStack, clickHandler);
    }


    // Implementation

    @Pure int width();
    @Pure int height();

    /**
     * Returns the items in the {@link View} in the views coordinate system. The array length must be equal to {@link #width()} * {@link #height()}.
     */
    @Pure
    @NotNull ItemStack[] getContents();

    @Pure
    boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType);

}
