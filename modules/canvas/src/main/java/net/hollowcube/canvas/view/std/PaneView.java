package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.ParentView;
import net.hollowcube.canvas.view.View;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PaneView implements ParentView {
    private final int width, height;

    private final @NotNull View @NotNull[] children;
    private final @NotNull Map<View, Integer> childMap = new HashMap<>();
    private final @NotNull ItemStack @NotNull[] content;

    public PaneView(int width, int height) {
        this.width = width;
        this.height = height;
        children = new View[width * height];
        content = new ItemStack[width * height];
        Arrays.fill(content, ItemStack.AIR);
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
    public @NotNull ItemStack[] getContents() {
        return content;
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        var child = children[slot];
        if (child == null) return ClickHandler.DENY;
        var offset = childMap.get(child);

        // Convert slot to child's coordinate system
        int x = slot % width(), y = slot / width();
        int cx = x - (offset % width()), cy = y - (offset / width());
        var childSlot = cx + child.width() * cy;

        return child.handleClick(player, childSlot, clickType);
    }

    @Override
    public void add(int x, int y, @NotNull View view) {
        Check.argCondition(x < 0, "view out of bounds (x={0} < 0)", x);
        Check.argCondition(y < 0, "view out of bounds (y={0} < 0)", y);
        Check.argCondition(x + view.width() > width, "view out of bounds (x={0} > {1})", x + view.width(), width());
        Check.argCondition(y + view.height() > height, "view out of bounds (y={0} > {1})", y + view.height(), height());

        // Ensure there is no overlap
        for (int rx = x; rx < x + view.width(); rx++) {
            for (int ry = y; ry < y + view.height(); ry++) {
                Check.argCondition(children[rx + ry * width()] != null, "view overlap: {0} at {1},{2}", view, x, y);
            }
        }

        // Actually add the component
        childMap.put(view, x + width * y);
        var contents = view.getContents();
        for (int rx = x; rx < x + view.width(); rx++) {
            for (int ry = y; ry < y + view.height(); ry++) {
                children[rx + width * ry] = view;
                content[rx + width * ry] = contents[(rx - x) + view.width() * (ry - y)];
            }
        }
    }

}
