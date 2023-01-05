package net.hollowcube.canvas;

import net.hollowcube.canvas.view.View;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SectionHostingView implements View {
    private final Section section;

    public SectionHostingView(Section section) {
        this.section = section;
    }

    @Override
    public int width() {
        return section.width();
    }

    @Override
    public int height() {
        return section.height();
    }

    @Override
    public @NotNull ItemStack[] getContents() {
        //todo this should trigger a mount, not sure how to handle unmount
        return new ItemStack[0];
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        return section.handleClick(slot, player, clickType);
    }
}
