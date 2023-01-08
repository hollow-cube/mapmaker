package net.hollowcube.canvas.view.std;

import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ItemViewTest {
    private static final ItemStack TEST_ITEM = ItemStack.builder(Material.DIAMOND)
            .meta(meta -> meta.customModelData(1))
            .displayName(Component.text("test123"))
            .build();

    @Test
    public void testSizeConstraints() {
        var view = new ItemView(1, 1, TEST_ITEM);
        assertEquals(1, view.width());
        assertEquals(1, view.height());
        assertEquals(1, view.getContents().length);
    }

    @Test
    public void testClickHandling() {
        var view = new ItemView(1, 1, TEST_ITEM);
        assertFalse(view.handleClick(null, 0, ClickType.LEFT_CLICK));
    }

    @Test
    public void testContents() {
        var view = new ItemView(1, 1, TEST_ITEM);
        assertEquals(TEST_ITEM, view.getContents()[0]);
    }

}
