package net.hollowcube.canvas.internal.standalone;

import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BoxContainer extends ContainerElement {

    public enum Align {
        LTR, TTB
    }

    private final Align align;

    public BoxContainer(@Nullable String id, int width, int height, @NotNull Align align) {
        super(id, width, height);
        this.align = Objects.requireNonNull(align, "alignment must be specified on BoxContainer");
    }

    protected BoxContainer(@NotNull BoxContainer other) {
        super(other);
        this.align = other.align;
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        var items = new ItemStack[width() * height()];

        if (align == Align.LTR) {
            int x = 0;
            for (var child : children()) {
                patchItemArray(items, width(), height(),
                        child.getContents(), x, 0,
                        child.width(), child.height());
                x += child.width();
            }
        } else if (align == Align.TTB) {
            int y = 0;
            for (var child : children()) {
                patchItemArray(items, width(), height(),
                        child.getContents(), 0, y,
                        child.width(), child.height());
                y += child.height();
            }
        } else {
            throw new IllegalStateException("Unsupported alignment: " + align);
        }

        return items;
    }

    @Override
    public @NotNull BoxContainer dup() {
        return new BoxContainer(this);
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
