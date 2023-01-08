package net.hollowcube.canvas.view;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//todo merge with TestView
public class MockView implements View {
    public static final ItemStack TEST_ITEM = ItemStack.of(Material.DIAMOND);

    private final int width, height;

    // Assertable state
    private final List<Integer> clickedSlots = new ArrayList<>();
    private int renderCount = 0;

    public MockView(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public MockView() {
        this(1, 1);
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
    public @NotNull View construct(@NotNull ViewContext context) {
        renderCount++;
        return View.super.construct(context);
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

    public void assertRendered(int times) {
        assertEquals(times, renderCount);
    }
}
