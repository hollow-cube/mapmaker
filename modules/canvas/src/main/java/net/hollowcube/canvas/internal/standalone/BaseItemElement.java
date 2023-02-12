package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.section.ItemSection;
import net.hollowcube.canvas.section.Section;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

abstract class BaseItemElement extends ItemSection implements BaseElement {
    // Keeps track of the intended items for this element
    // This is used to handle updating items while loading.
    private final ItemStack @NotNull[] items;

    private final String id;
    private boolean loading = false;

    public BaseItemElement(@Nullable String id, int width, int height) {
        super(width, height);
        this.id = id;

        this.items = new ItemStack[width() * height()];
        Arrays.fill(items, ItemStack.AIR);
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    @Override
    public void setLoading(boolean loading) {
        if (this.loading == loading) return;
        this.loading = loading;

        if (this.loading) {
            // Started loading
            for (int i = 0; i < width() * height(); i++) {
                super.setItem(i, ItemStack.of(Material.BARRIER));
            }
        } else {
            // Stopped loading
            for (int i = 0; i < width() * height(); i++) {
                super.setItem(i, items[i]);
            }
        }
    }

    @Override
    public @NotNull Section section() {
        return this;
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
    }

    // Impl

    @Override
    protected void mount() {
        super.mount();

        // Draw sprite if present
        if (sprite != null) {
            find(RootElement.class).addSprite(this, sprite, 0);
        }
    }

    @Override
    protected void unmount() {
        super.unmount();

        find(RootElement.class).removeSprites(this);
    }

    @Override
    protected void setItem(int index, @NotNull ItemStack item) {
        // Always update local items
        Check.argCondition(index < 0 || index >= width() * height(), "index out of bounds");
        items[index] = item;

        // Update in parent only if not loading
        if (!loading) {
            super.setItem(index, item);
        }
    }

    protected void setItem(@NotNull ItemStack itemStack) {
        for (int i = 0; i < width() * height(); i++) {
            setItem(i, itemStack);
        }
    }

    @Override
    public abstract BaseElement clone();
}
