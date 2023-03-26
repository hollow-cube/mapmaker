package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public abstract class BaseElement implements Element {

    protected final ElementContext context;
    protected @Nullable String id;
    private final int width;
    private final int height;

    protected BaseElement(@NotNull ElementContext context, @Nullable String id, int width, int height) {
        this.context = context;
        this.id = id;
        this.width = width;
        this.height = height;
    }

    protected BaseElement(@NotNull ElementContext context, @NotNull BaseElement other) {
        this.context = context;
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

    public @Nullable BaseElement findById(@NotNull String id) {
        if (id.equals(this.id))
            return this;
        return null;
    }

    /**
     * Called when a player clicks on an item in the inventory. The slot is local to the element,
     * eg a 1x1 element will only ever have clicks on slot 0.
     *
     * @param player The player who clicked the slot in the inventory.
     * @param slot The slot they clicked, local to this element
     * @param clickType The type of click they did
     * @return {@link #CLICK_ALLOW} or {@link #CLICK_DENY}
     */
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        return CLICK_DENY;
    }

    public void wireAction(@NotNull View view, @NotNull Method method) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support actions.");
    }

    // TRAIT: DepthAware

    private int zIndex = 0;

    public int zIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    // TRAIT: SpriteHolder

    private Sprite sprite = null; //todo draw sprite

    public void setSprite(@Nullable Sprite sprite) {
        this.sprite = sprite;
    }


    public abstract @NotNull BaseElement clone(@NotNull ElementContext context);
}
