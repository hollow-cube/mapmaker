package net.hollowcube.canvas.std;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ItemSection;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ButtonSection extends ItemSection {
    private ClickHandler onClick;

    public ButtonSection(int width, int height, @NotNull ItemStack item) {
        this(width, height, item, (ClickHandler) null);
    }

    public ButtonSection(int width, int height, @NotNull ItemStack item, @Nullable Runnable onClick) {
        this(width, height, item, (ClickHandler) null);
        if (onClick != null) setOnClick(onClick);
    }

    public ButtonSection(int width, int height, @NotNull ItemStack item, @Nullable ClickHandler onClick) {
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

    protected void setClickHandler(@NotNull ClickHandler handler) {
        this.onClick = handler;
    }

    protected void setOnClick(@NotNull Runnable onClick) {
        this.onClick = (player, slot, clickType) -> {
            if (clickType == ClickType.LEFT_CLICK) {
                onClick.run();
            }
            return ClickHandler.DENY;
        };
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        return onClick.handleClick(player, slot, clickType);
    }
}
