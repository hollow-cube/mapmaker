package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public record ButtonView(
        int width, int height,
        @NotNull ItemStack itemStack,
        @NotNull ClickHandler clickHandler
) implements View {

    public ButtonView(int width, int height, @NotNull Material material, int cmd, @NotNull String key, @NotNull List<Component> args, @NotNull ClickHandler clickHandler) {
        this(width, height, ItemStack.builder(material)
                .displayName(Component.translatable(key + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(key + ".lore", args))
                .meta(meta -> {if (cmd != 0) meta.customModelData(cmd);})
                .build(),
                clickHandler);
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
        var items = new ItemStack[width * height];
        Arrays.fill(items, itemStack);
        return items;
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        return clickHandler.handleClick(player, slot, clickType);
    }

}
