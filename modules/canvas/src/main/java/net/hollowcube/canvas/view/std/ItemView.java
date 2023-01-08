package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public record ItemView(int width, int height, @NotNull ItemStack itemStack) implements View {

    public ItemView(int width, int height, @NotNull ItemStack baseItem, @NotNull String key, @NotNull List<Component> args) {
        this(width, height, baseItem
                .withDisplayName(Component.translatable(key + ".name", args))
                .withLore(LanguageProvider.optionalMultiTranslatable(key + ".lore", args)));
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
        return ClickHandler.DENY;
    }
}
