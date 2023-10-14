package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public abstract class BaseElement implements Element {
    public static final ItemStack LOADING_ITEM = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(2))
            .build();


    protected final ElementContext context;
    protected @Nullable String id;
    private final int width;
    private final int height;

    private State state = State.ACTIVE;

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

        this.zIndex = other.zIndex;
        this.sprite = other.sprite;
        this.loadingSprite = other.loadingSprite;
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

    public boolean shouldDelegateDraw() {
        return state == State.LOADING || state == State.DISABLED;
    }

    public boolean shouldIgnoreInput() {
        return state == State.LOADING || state == State.DISABLED;
    }

    @Override
    public void setLoading(boolean loading) {
        setState(loading ? State.LOADING : State.ACTIVE);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void setState(@NotNull State state) {
        if (this.state == state) return;
        this.state = state;
        context.markDirty();
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
    }

    /**
     * getContents gets the items within the element.
     * <p>
     * The result array is a flat 2d array of all the items in the slot, or null if an item is not set.
     * The length of the array _must_ be {@link #width()} * {@link #height()}.
     */
    public @Nullable ItemStack @NotNull [] getContents() {
        var items = new ItemStack[width * height];
        if (state == State.LOADING && loadingSprite == null) {
            Arrays.fill(items, LOADING_ITEM);
        }
        return items;
    }

    public void buildTitle(@NotNull FontUIBuilder sb, int x, int y) {
        drawBackgroundSprite(sb, x, y);
    }

    protected void drawBackgroundSprite(@NotNull FontUIBuilder sb, int x, int y) {
        if (state == State.ACTIVE && sprite != null) {
            sb.draw(sprite, x);
//            sb.append(FontUtil.computeOffset(sprite.offsetX() + (x * 18)));
//            sb.append(sprite.fontChar());
//            sb.append(FontUtil.computeOffset(-(sprite.offsetX() + sprite.width() + 1 + (x * 18))));
//            System.out.println("DREW");
        }

        if (state == State.LOADING && loadingSprite != null) {
            sb.draw(loadingSprite, 0);
//            sb.append(FontUtil.computeOffset(loadingSprite.offsetX()));
//            sb.append(loadingSprite.fontChar());
//            sb.append(FontUtil.computeOffset(-(loadingSprite.offsetX() + loadingSprite.width())));
        }
    }

    public @Nullable BaseElement findById(@NotNull String id) {
        if (id.equals(this.id))
            return this;
        return null;
    }

    public void collectById(@NotNull Predicate<String> predicate, @NotNull List<Element> result) {
        if (id != null && predicate.test(id))
            result.add(this);
    }

    /**
     * Called when a player clicks on an item in the inventory. The slot is local to the element,
     * eg a 1x1 element will only ever have clicks on slot 0.
     *
     * @param player    The player who clicked the slot in the inventory.
     * @param slot      The slot they clicked, local to this element
     * @param clickType The type of click they did
     * @return {@link #CLICK_ALLOW} or {@link #CLICK_DENY}
     */
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        return CLICK_DENY;
    }

    public void wireAction(@NotNull View view, @NotNull Object handler, @NotNull Action.Descriptor action) {
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

    private Sprite sprite = null;

    public void setSprite(@Nullable Sprite sprite) {
        this.sprite = sprite;
        context.markDirty();
    }

    // TRAIT: LoadingSpriteHolder

    private Sprite loadingSprite = null;

    public void setLoadingSprite(@Nullable Sprite sprite) {
        this.loadingSprite = sprite;
        context.markDirty();
    }

    /**
     * Returns whether this or any child component is in a loading state.
     */
    public boolean isAnyLoading() {
        return state == State.LOADING;
    }

    public abstract @NotNull BaseElement clone(@NotNull ElementContext context);
}
