package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.ClickHandler;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ButtonViewTest {
    private static final ItemStack TEST_ITEM = ItemStack.builder(Material.DIAMOND)
            .meta(meta -> meta.customModelData(1))
            .displayName(Component.text("test123"))
            .build();

    @Test
    public void testSizeConstraints() {
        int width = 5, height = 5;
        var view = new ButtonView(width, height, TEST_ITEM, ClickHandler.noop());
        assertEquals(width, view.width());
        assertEquals(height, view.height());
        assertEquals(width * height, view.getContents().length);
    }

    @Test
    public void testClickHandling() {
        var clicked = new AtomicBoolean(false);
        var view = new ButtonView(1, 1, TEST_ITEM, (player, slot, clickType) -> {
            clicked.set(true);
            return true;
        });
        var action = view.handleClick(null, 0, ClickType.LEFT_CLICK);
        assertTrue(action);
        assertTrue(clicked.get());
    }

    @Test
    public void testContents1() {
        var view = new ButtonView(1, 1, TEST_ITEM, ClickHandler.noop());
        assertEquals(TEST_ITEM, view.getContents()[0]);
    }

    @Test
    public void testContents2() {
        var view = new ButtonView(5, 5, TEST_ITEM, ClickHandler.noop());
        for (var item : view.getContents()) {
            assertEquals(TEST_ITEM, item);
        }
    }

}
