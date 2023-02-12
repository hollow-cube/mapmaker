package net.hollowcube.canvas.section;

import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Base class for any {@link Section} that contains {@link net.minestom.server.item.ItemStack}s directly.
 */
public non-sealed class ItemSection extends Section {
    private final ItemStack @NotNull[] items;

    public ItemSection(int width, int height) {
        super(width, height);
        this.items = new ItemStack[width * height];
        Arrays.fill(items, ItemStack.AIR);
    }

    /**
     * Sets the given index to the given {@link net.minestom.server.item.ItemStack}.
     * <p>
     * The index is local to this component.
     *
     * @throws IllegalArgumentException if the index is out of bounds
     */
    protected void setItem(int index, @NotNull ItemStack item) {
        Check.argCondition(index < 0 || index >= width() * height(), "index out of bounds");
        items[index] = item;
        if (isMounted()) {
            parent().updateItem(getIndexInParent(index), item);
        }
    }

    protected @NotNull ItemStack getItem(int index) {
        Check.argCondition(index < 0 || index >= width() * height(), "index out of bounds");
        return items[index];
    }

    @Override
    protected void mount() {
        super.mount();

        // Update all items in parent
        for (int i = 0; i < items.length; i++) {
            var item = items[i];
            // No need to update when air, parent has already cleared this space
            if (item.isAir()) continue;
            parent().updateItem(getIndexInParent(i), items[i]);
        }
    }
}
