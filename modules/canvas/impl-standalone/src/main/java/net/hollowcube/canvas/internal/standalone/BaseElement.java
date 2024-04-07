package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.internal.standalone.trait.Loadable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;

public abstract class BaseElement implements Element, Loadable {
    public static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    public static final ItemStack LOADING_BROKEN_ITEM = ItemStack.builder(Material.STICK)
            .displayName(Component.text(""))
            .meta(meta -> meta.customModelData(2))
            .build();
    public static final ItemStack LOADING_SPINNER_ITEM = ItemStack.builder(Material.STICK)
            .displayName(Component.text(""))
            .meta(meta -> meta.customModelData(3))
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

        this.loadingType = other.loadingType;
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
            getLoadingContent(items);
        }
        return items;
    }

    public void buildTitle(@NotNull FontUIBuilder sb, int x, int y) {
        drawBackgroundSprite(sb, x, y);
    }

    protected void drawBackgroundSprite(@NotNull FontUIBuilder sb, int x, int y) {
        if (state == State.ACTIVE && sprite != null) {
            sb.draw(sprite, x, spriteColorModifier);
        }

        if (state == State.LOADING && loadingSprite != null) {
            sb.draw(loadingSprite, 0);
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
    public @Nullable Future<Void> handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        return null;
    }

    public void wireAction(@NotNull View view, @NotNull Object handler, @NotNull Action.Descriptor action) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support actions.");
    }

    // TRAIT: Loadable

    private String loadingType = "default";

    @Override
    public void setLoadingType(@NotNull String loadingType) {
        this.loadingType = loadingType;
    }

    private void getLoadingContent(ItemStack[] items) {
        switch (loadingType) {
            case "default" -> Arrays.fill(items, LOADING_BROKEN_ITEM);
            case "centered" -> {
                Arrays.fill(items, ItemStack.AIR);
                int x = width / 2;
                int y = height / 2;
                items[x + (y * width)] = LOADING_SPINNER_ITEM;
            }
            default -> throw new IllegalStateException("Unknown loading type: " + loadingType);
        }
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
    private TextColor spriteColorModifier = null;

    public void setSprite(@Nullable Sprite sprite) {
        this.sprite = sprite;
        context.markDirty();
    }

    // This actually does override a method in Label
    public void setSpriteColorModifier(@NotNull TextColor color) {
        this.spriteColorModifier = color;
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
