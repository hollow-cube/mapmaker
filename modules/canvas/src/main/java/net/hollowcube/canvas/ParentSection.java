package net.hollowcube.canvas;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Section} that can contain other {@link Section}s.
 */
public non-sealed class ParentSection extends Section {
    private final Map<Integer, Section> children = new HashMap<>();
    private final @Nullable Section[] childMap;

    public ParentSection(int width, int height) {
        super(width, height);
        this.childMap = new Section[width * height];
    }

    // Public API

    protected <C extends Section> C add(int index, @NotNull C comp) {
        return add(index % width(), index / width(), comp);
    }

    protected <C extends Section> C add(int x, int y, @NotNull C comp) {
        mountChild(x, y, comp);
        return comp;
    }

    protected void mountChild(int x, int y, @NotNull Section comp) {
        var offset = x + width() * y;
        children.put(offset, comp);
        for (int i = 0; i < comp.width(); i++) {
            for (int j = 0; j < comp.height(); j++) {
                childMap[(x + i) + width() * (y + j)] = comp;
            }
        }

        if (isMounted()) {
            comp.setParent(this, offset);
        }
    }

    protected void unmountChild(int x, int y, @NotNull Section comp) {
        var offset = x + width() * y;
        children.remove(offset);
        for (int i = 0; i < comp.width(); i++) {
            for (int j = 0; j < comp.height(); j++) {
                childMap[(x + i) + width() * (y + j)] = null;
            }
        }

        if (isMounted()) {
            comp.removeParent();
        }
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        var child = childMap[slot];
        if (child == null) return false;

        // Convert slot to child's coordinate system
        int x = slot % 9, y = slot / 9;
        int cx = x - (child.offset() % 9), cy = y - (child.offset() / 9);
        slot = cx + 9 * cy;

        return child.handleClick(slot, player, clickType);
    }


    // Implementation details


    @Override
    protected void mount() {
        super.mount();

        for (var entry : children.entrySet()) {
            entry.getValue().setParent(this, entry.getKey());
        }
    }

    @Override
    protected void unmount() {
        super.unmount();

        for (var entry : children.entrySet()) {
            entry.getValue().removeParent();
        }
//        for (var child : children) {
            //todo
//            child.setParent(null, child.offset());
//        }
    }

    /**
     * Updates the item in the root component.
     *
     * @apiNote Only available when the component is mounted.
     */
    void updateItem(int index, @NotNull ItemStack itemStack) {
        parent().updateItem(getIndexInParent(index), itemStack);
    }
}
