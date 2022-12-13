package net.hollowcube.canvas.std;

import net.hollowcube.canvas.ItemSection;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ButtonSection extends ItemSection {
    private final Runnable onClick;

    public ButtonSection(int width, int height, @NotNull ItemStack item, @NotNull Runnable onClick) {
        super(width, height);
        this.onClick = onClick;

        setItem(item);
    }

    public void setItem(@NotNull ItemStack item) {
        for (int i = 0; i < width() * height(); i++) {
            setItem(i, item);
        }
    }

    public @NotNull ItemStack getItem() {
        return getItem(0);
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK)
            onClick.run();
        return false;
    }
}
