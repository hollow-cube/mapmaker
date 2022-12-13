package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class BigInventoryDemo extends ParentSection {
    public BigInventoryDemo() {
        super(9, 9);

        for (int i = 0; i < width() * height(); i++) {
            final int num = i;
            add(i, new ButtonSection(1, 1, ItemStack.of(Material.fromId(i + 10)), () -> {
                System.out.println("Clicked: " + num);
            }));
        }
    }
}
