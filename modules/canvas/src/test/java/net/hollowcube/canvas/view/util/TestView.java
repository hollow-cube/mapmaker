package net.hollowcube.canvas.view.util;

import net.hollowcube.canvas.view.View;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public record TestView(int width, int height, List<Integer> clickedSlots) implements View {
    public static final ItemStack TEST_ITEM = ItemStack.of(Material.DIAMOND);

    public TestView(int width, int height) {
        this(width, height, new ArrayList<>());
    }

    @Override
    public @NotNull ItemStack[] getContents() {
        var contents = new ItemStack[width * height];
        Arrays.fill(contents, TEST_ITEM);
        return contents;
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        clickedSlots.add(slot);
        return false;
    }

    public void assertClicked(int... slots) {
        assertEquals(slots.length, clickedSlots.size());

        for (int i = 0; i < slots.length; i++) {
            assertEquals(slots[i], clickedSlots.get(i), "Slot " + i + " did not match: " + slots[i] + " != " + clickedSlots.get(i));
        }
    }
}
