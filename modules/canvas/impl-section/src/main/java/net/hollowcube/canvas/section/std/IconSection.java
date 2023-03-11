package net.hollowcube.canvas.section.std;

import net.hollowcube.canvas.section.ItemSection;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class IconSection extends ItemSection {
    public IconSection(@NotNull ItemStack item) {
        super(1, 1);
        setItem(0, item);
    }

    public void setItem(@NotNull ItemStack item) {
        setItem(0, item);
    }
}
