package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Base class for elements that can contain other elements. By default, layout is done
 * left-to-right and top-to-bottom.
 */
public class ContainerElement extends BaseElement {

    private final List<BaseElement> children = new ArrayList<>();

    public ContainerElement(@NotNull ElementContext context, @Nullable String id, int width, int height) {
        super(context, id, width, height);
    }

    protected ContainerElement(@NotNull ElementContext context, @NotNull ContainerElement other) {
        super(context, other);
        for (var child : other.children) {
            children.add(child.clone(context));
        }
    }

    public void forEachChild(@NotNull Consumer<BaseElement> consumer) {
        for (var child : children) {
            consumer.accept(child);
        }
    }

    @Override
    public boolean isAnyLoading() {
        if (super.isAnyLoading()) return true;

        for (var child : children) {
            if (child.isAnyLoading()) return true;
        }

        return false;
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

            patchItemArray(this,
                    items, width(), height(),
                    child.getContents(), x, y,
                    child.width(), child.height());
            x += child.width();
            nextY = Math.max(nextY, y + child.height());
        }
        return items;
    }

    @Override
    public @Nullable Future<Void> handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (shouldIgnoreInput()) return null;

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

        return null;
    }

    @Override
    public void buildTitle(@NotNull FontUIBuilder sb, int componentX, int componentY) {
        drawBackgroundSprite(sb, componentX, componentY);

        int x = 0, y = 0, nextY = 0;
        for (var child : children()) {
            if (x >= width()) {
                x = 0;
                y = nextY;
            }

            child.buildTitle(sb, componentX + x, componentY + y);
            x += child.width();
            nextY = Math.max(nextY, y + child.height());
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

    @Override
    public void collectById(@NotNull Predicate<String> predicate, @NotNull List<Element> result) {
        super.collectById(predicate, result);

        for (var child : children) {
            // If the child is a view, we only check that ID.
            // This is to prevent searching inner views for IDs (aka implement id scoping).
            if (child instanceof ViewContainer) {
                if (child.id() != null && predicate.test(child.id()))
                    result.add(child);
                continue;
            }

            child.collectById(predicate, result);
        }
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
            @NotNull Element self,
            @Nullable ItemStack @NotNull [] items,
            int itemsWidth, int itemsHeight,
            @Nullable ItemStack @NotNull [] patch,
            int patchX, int patchY,
            int patchWidth, int patchHeight
    ) {
        try {

            for (int y = 0; y < patchHeight; y++) {
                for (int x = 0; x < patchWidth; x++) {
                    var item = patch[y * patchWidth + x]; // NOSONAR - it doesnt understand nullable here
                    if (item != null) {
                        items[(patchY + y) * itemsWidth + (patchX + x)] = item; // NOSONAR - see above
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Patch out of bounds for " + self.id(), e);
        }
    }
}
