package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for elements that can contain other elements. By default, layout is done
 * left-to-right and top-to-bottom.
 */
public class ContainerElement extends BaseElement {

    private List<BaseElement> children = new ArrayList<>();

    public ContainerElement(@NotNull ElementContext context, @Nullable String id, int width, int height) {
        super(context, id, width, height);
    }

    protected ContainerElement(@NotNull ElementContext context, @NotNull ContainerElement other) {
        super(context, other);
        for (var child : other.children) {
            children.add(child.clone(context));
        }
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        if (shouldDelegateDraw()) return super.getContents();

        var items = new ItemStack[width() * height()];

        //todo this needs a bunch of bounds checks

        int x = 0, y = 0, nextY = 0;
        for (var child : children()) {
            if (x >= width()) {
                x = 0;
                y = nextY;
            }

            patchItemArray(items, width(), height(),
                    child.getContents(), x, y,
                    child.width(), child.height());
            x += child.width();
            nextY = Math.max(nextY, y + child.height());
        }
        return items;
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (shouldIgnoreInput()) return CLICK_DENY;

        int cx = slot % width();
        int cy = slot / width();

        int x = 0, y = 0, nextY = 0;
        for (var child : children()) {
            if (x >= width()) {
                x = 0;
                y = nextY;
            }

            if (cx >= x && cx < x + child.width() && cy >= y && cy < y + child.height()) {
                return child.handleClick(player, (cx - x) + (cy - y) * child.width(), clickType);
            }

            x += child.width();
            nextY = Math.max(nextY, y + child.height());
        }

        return CLICK_DENY;
    }

    @Override
    public void buildTitle(@NotNull StringBuilder sb) {
        super.buildTitle(sb);
        for (var child : children) {
            child.buildTitle(sb);
        }
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        for (var child : children) {
            child.performSignal(name, args);
        }
    }

    @Override
    public @Nullable BaseElement findById(@NotNull String id) {
        var found = super.findById(id);
        if (found != null) return found;

        for (var child : children) {
            // If the child is a view, we only check that ID.
            // This is to prevent searching inner views for IDs (aka implement id scoping).
            if (child instanceof ViewContainer) {
                if (id.equals(child.id()))
                    return child;
                continue;
            }

            // Otherwise, search the child recursively.
            found = child.findById(id);
            if (found != null) return found;
        }

        return null;
    }

    public @NotNull List<@NotNull BaseElement> children() {
        return List.copyOf(children);
    }

    public void addChild(@NotNull BaseElement child) {
        children.add(child);
    }

    @Override
    public @NotNull ContainerElement clone(@NotNull ElementContext context) {
        return new ContainerElement(context, this);
    }

    static void patchItemArray(
            @Nullable ItemStack @NotNull[] items,
            int itemsWidth, int itemsHeight,
            @Nullable ItemStack @NotNull[] patch,
            int patchX, int patchY,
            int patchWidth, int patchHeight
    ) {
        for (int y = 0; y < patchHeight; y++) {
            for (int x = 0; x < patchWidth; x++) {
                var item = patch[y * patchWidth + x]; // NOSONAR - it doesnt understand nullable here
                if (item != null) {
                    items[(patchY + y) * itemsWidth + (patchX + x)] = item; // NOSONAR - see above
                }
            }
        }
    }
}
