package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseElement implements Element {

    protected @Nullable String id;
    private final int width;
    private final int height;

    protected BaseElement(@Nullable String id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    protected BaseElement(@NotNull BaseElement other) {
        this.id = other.id;
        this.width = other.width;
        this.height = other.height;
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public void setLoading(boolean loading) {
        throw new UnsupportedOperationException("todo not implemented");
    }

    /**
     * getContents gets the items within the element.
     * <p>
     * The result array is a flat 2d array of all the items in the slot, or null if an item is not set.
     * The length of the array _must_ be {@link #width()} * {@link #height()}.
     */
    public abstract @Nullable ItemStack @NotNull [] getContents();

    public abstract @NotNull BaseElement dup();
}
