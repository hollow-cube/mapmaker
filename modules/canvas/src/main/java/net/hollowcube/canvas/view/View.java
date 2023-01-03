package net.hollowcube.canvas.view;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.std.ButtonView;
import net.hollowcube.canvas.view.std.ItemView;
import net.hollowcube.canvas.view.std.PaneView;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    static @NotNull View Button(@NotNull ItemStack itemStack, @NotNull ClickHandler clickHandler) {
        return Button(1, 1, itemStack, clickHandler);
    }

    @Pure
    static @NotNull View Button(int width, int height, @NotNull ItemStack itemStack, @NotNull ClickHandler clickHandler) {
        return new ButtonView(width, height, itemStack, clickHandler);
    }


    // TranslatableButton

    @Pure
    static @NotNull View TranslatedButton(@NotNull Material material, @NotNull String translationKey, @NotNull List<Component> args, @NotNull ClickHandler onClick) {
        return TranslatedButton(1, 1, material, 0, translationKey, args, onClick);
    }

    @Pure
    static @NotNull View TranslatedButton(@NotNull Material material, int cmd, @NotNull String translationKey, @NotNull List<Component> args, @NotNull ClickHandler onClick) {
        return TranslatedButton(1, 1, material, cmd, translationKey, args, onClick);
    }

    @Pure
    static @NotNull View TranslatedButton(int width, int height, @NotNull Material material, @NotNull String translationKey, @NotNull List<Component> args, @NotNull ClickHandler clickHandler) {
        return TranslatedButton(width, height, material, 0, translationKey, args, clickHandler);
    }

    @Pure
    static @NotNull View TranslatedButton(int width, int height, @NotNull Material material, int cmd, @NotNull String translationKey, @NotNull List<Component> args, @NotNull ClickHandler clickHandler) {
        return new ButtonView(width, height, material, cmd, translationKey, args, clickHandler);
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
