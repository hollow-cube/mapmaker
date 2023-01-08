package net.hollowcube.canvas.view;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.std.ButtonView;
import net.hollowcube.canvas.view.std.FutureSupport;
import net.hollowcube.canvas.view.std.ItemView;
import net.hollowcube.canvas.view.std.PaneView;
import net.hollowcube.common.result.FutureResult;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface View extends ViewFunc {

    // Pane/group

    @Pure
    static @NotNull ParentView Pane(int width, int height) {
        return new PaneView(width, height);
    }


    // Single item

    @Pure
    static @NotNull View Item(@NotNull ItemStack itemStack) {
        return new ItemView(1, 1, itemStack);
    }

    @Pure
    static @NotNull View Item(int width, int height, @NotNull ItemStack itemStack) {
        return new ItemView(width, height, itemStack);
    }


    // Translated Item

    @Pure
    static @NotNull View TranslatedItem(@NotNull Material mat, @NotNull String translationKey, @NotNull List<Component> args) {
        return TranslatedItem(1, 1, ItemStack.of(mat), translationKey, args);
    }

    @Pure
    static @NotNull View TranslatedItem(@NotNull ItemStack baseItem, @NotNull String translationKey, @NotNull List<Component> args) {
        return TranslatedItem(1, 1, baseItem, translationKey, args);
    }

    @Pure
    static @NotNull View TranslatedItem(int width, int height, @NotNull ItemStack baseItem, @NotNull String translationKey, @NotNull List<Component> args) {
        return new ItemView(width, height, baseItem, translationKey, args);
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

    @Pure
    static @NotNull View Button(@NotNull ViewContext context, @NotNull ViewFunc viewFunc, @NotNull ClickHandler clickHandler) {
        return new ButtonView(viewFunc.construct(context), clickHandler);
    }


    // TranslatableButton

    @Pure
    static @NotNull View TranslatedButton(@NotNull Material mat, @NotNull String translationKey, @NotNull List<Component> args, @NotNull ClickHandler onClick) {
        return TranslatedButton(1, 1, ItemStack.of(mat), translationKey, args, onClick);
    }

    @Pure
    static @NotNull View TranslatedButton(@NotNull ItemStack baseItem, @NotNull String translationKey, @NotNull List<Component> args, @NotNull ClickHandler onClick) {
        return TranslatedButton(1, 1, baseItem, translationKey, args, onClick);
    }

    @Pure
    static @NotNull View TranslatedButton(int width, int height, @NotNull ItemStack baseItem, @NotNull String translationKey, @NotNull List<Component> args, @NotNull ClickHandler clickHandler) {
        return new ButtonView(width, height, baseItem, translationKey, args, clickHandler);
    }


    // Loading
    // todo add a second way with a default/generic error view.

    @Pure
    static @NotNull View Loading(@NotNull ViewContext context, @NotNull FutureResult<?> future, @NotNull ViewFunc loading, @NotNull ViewFunc loaded, @NotNull ViewFunc error) {
        return FutureSupport.Loading(context, future, loading, loaded, error);
    }


    // Pagination

//    @Pure
//    static @NotNull View Pagination(@NotNull ViewContext context, @NotNull Pagination.Controller controller) {
//
//    }


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


    //todo may remove this, but it is kinda convenient in some cases for a view to also be a ViewFunc
    @Override
    default @NotNull View construct(@NotNull ViewContext context) { return this; }
}
