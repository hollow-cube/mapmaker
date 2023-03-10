package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.section.ParentSection;
import net.hollowcube.canvas.section.Section;
import net.hollowcube.canvas.section.std.ButtonSection;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

abstract class BaseParentElement extends ParentSection implements BaseElement {

    // We cache all children locally to handle loading transparently to any implementing components.
    // Note: the only safe child manipulation methods are `super.mountChild(int, int, Section)` and `super.unmountChild(int, int, Section)`.
    private final Map<Integer, Section> children = new HashMap<>();
    private final @Nullable Section[] childMap;

    //todo this is non final because RootElement has a setter for it which is used to preserve ID
    // when instantiating an imported element. This is a pretty yikes hack that should be improved
    // sometime in the future (eg when rewriting not to be built on top of the old section gui impl)
    protected String id;
    private boolean loading = false;
    private ButtonSection loadingButton;

    private View associatedView = null;

    public BaseParentElement(@Nullable String id, int width, int height) {
        super(width, height);
        this.id = id;

        this.childMap = new Section[width() * height()];
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    @Override
    public void setLoading(boolean loading) {
        if (this.loading == loading) return;
        this.loading = loading;

        if (loading) {
            // Started loading

            for (var entry : children.entrySet()) {
                var index = entry.getKey();
                var x = index % width();
                var y = index / width();
                super.unmountChild(x, y, entry.getValue());
            }

            loadingButton = new ButtonSection(width(), height(), ItemStack.of(Material.BARRIER));
            super.mountChild(0, 0, loadingButton);
        } else {
            // Stopped loading

            super.unmountChild(0, 0, loadingButton);
            loadingButton = null;

            for (var entry : children.entrySet()) {
                var index = entry.getKey();
                var x = index % width();
                var y = index / width();
                super.mountChild(x, y, entry.getValue());
            }
        }
    }

    @Override
    public void setAssociatedView(@Nullable View associatedView) {
        this.associatedView = associatedView;
    }

    @Override
    public @Nullable View getAssociatedView() {
        return associatedView;
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

        if (associatedView != null) {
            associatedView.mount();
        }
    }

    @Override
    protected void unmount() {
        super.unmount();

        find(RootElement.class).removeSprites(this);
    }

    @Override
    protected @Nullable Section get(int index) {
        checkBounds(index);
        return childMap[index];
    }

    protected void mountChild(int x, int y, @NotNull Section comp) {
        // Always add the child to local cache to handle loading
        var offset = x + width() * y;
        children.put(offset, comp);
        for (int i = 0; i < comp.width(); i++) {
            for (int j = 0; j < comp.height(); j++) {
                childMap[(x + i) + width() * (y + j)] = comp;
            }
        }

        // If not loading, update in parent
        if (!loading) {
            super.mountChild(x, y, comp);
        }
    }

    protected void unmountChild(int x, int y, @NotNull Section comp) {
        // Always remove from local cache to handle loading
        var offset = x + width() * y;
        children.remove(offset);
        for (int i = 0; i < comp.width(); i++) {
            for (int j = 0; j < comp.height(); j++) {
                childMap[(x + i) + width() * (y + j)] = null;
            }
        }

        // If not loading, also remove from parent
        if (!loading) {
            super.unmountChild(x, y, comp);
        }
    }

    protected void unmountChild(int index) {
        checkBounds(index);
        var section = childMap[index];
        if (section == null) return;
        unmountChild(index % width(), index / width(), section);
    }


    @Override
    public abstract BaseElement clone();

}
