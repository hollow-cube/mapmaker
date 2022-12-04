package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.canvas.std.IconSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class CounterDemo extends ParentSection {
    private static final ItemStack DECREMENT_ITEM = ItemStack.of(Material.RED_STAINED_GLASS_PANE);
    private static final ItemStack INCREMENT_ITEM = ItemStack.of(Material.GREEN_STAINED_GLASS_PANE);

    private final IconSection icon;
    private int counter = 0;

    public CounterDemo() {
        super(9, 1);

        // Controls
        add(0, 0, new ButtonSection(4, 1, DECREMENT_ITEM, () -> updateCounter(-1)));
        add(5, 0, new ButtonSection(4, 1, INCREMENT_ITEM, () -> updateCounter(+1)));

        // Counter icon
        icon = add(4, 0, new IconSection(createItem()));
    }

    private void updateCounter(int delta) {
        counter += delta;
        icon.setItem(createItem());
    }

    private @NotNull ItemStack createItem() {
        return ItemStack.of(Material.PAPER)
                .withDisplayName(Component.text("Counter: " + counter));
    }

}
