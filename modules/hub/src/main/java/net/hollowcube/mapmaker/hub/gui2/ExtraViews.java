package net.hollowcube.mapmaker.hub.gui2;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.common.result.Error;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ExtraViews {
    private ExtraViews() {}

    private static final ItemStack BACK_ITEM = ItemStack.builder(Material.ARROW)
            .displayName(Component.translatable("gui.generic.back.name"))
            .build();

    private static final ItemStack CLOSE_ITEM = ItemStack.builder(Material.STRUCTURE_VOID)
            .displayName(Component.translatable("gui.generic.close.name"))
            .build();

    public static @NotNull View BackButton(@NotNull ViewContext context) {
        var itemStack = context.hasHistory() ? BACK_ITEM : CLOSE_ITEM;
        return View.Button(itemStack, ClickHandler.leftClick(context::popView));
    }

    public static @NotNull View InfoButton(@NotNull String name) {
        var key = String.format("gui.%s.info", name);
        return View.Item(ItemStack.of(Material.PAPER)
                .withDisplayName(Component.translatable(String.format("%s.name", key)))
                .withLore(LanguageProvider.optionalMultiTranslatable(String.format("%s.lore", key), List.of())));
    }

    public static @NotNull View Error(int width, int height, @NotNull Error err) {
        var pane = View.Pane(width, height);
        //todo
        pane.add(0, 0, View.Item(ItemStack.of(Material.PAPER).withDisplayName(Component.text("Error: " + err.message()))));
        return pane;
    }
}
