package net.hollowcube.canvas;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public sealed abstract class Section permits ItemSection, ParentSection {
    private final int width, height;

    private ParentSection parent = null;
    private int offset = -1;

    protected Section(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // Public API

    public final int width() {
        return _width();
    }

    public final int height() {
        return _height();
    }

    public <C extends Section> @UnknownNullability C find(Class<C> componentType) {
        Check.stateCondition(!isMounted(), "find on unmounted component");
        if (componentType.isAssignableFrom(getClass())) return componentType.cast(this);
        return parent.find(componentType);
    }

    /** Callback when the component is mounted in a parent */
    protected void mount() {}
    /** Callback when the component is unmounted from a parent */
    protected void unmount() {}

    /**
     * Implemented to handle clicking on the item.
     * @return True to allow taking the item, false to deny
     */
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        return true;
    }

    protected int getIndexInParent(int index) {
        return offset() + (index % width()) + (parent().width() * (index / width()));
    }

    // Implementation details

    /** Exists for RouterComponent to override, not a big fan of this at all. */
    int _width() {
        return width;
    }

    /** Exists for RouterComponent to override, not a big fan of this at all. */
    int _height() {
        return height;
    }

    protected boolean isMounted() {
        return parent != null;
    }

    protected int offset() {
        Check.stateCondition(!isMounted(), "component is not mounted");
        return offset;
    }

    /**
     * Gets the parent component, may only be used on a mounted component.
     */
    protected @NotNull ParentSection parent() {
        Check.stateCondition(!isMounted(), "component is not mounted");
        return parent;
    }

    protected void setParent(@NotNull ParentSection parent, int offset) {
        Check.stateCondition(isMounted(), "component is already mounted");
        this.parent = parent;
        this.offset = offset;
        mount();
    }

    protected void removeParent() {
        Check.stateCondition(!isMounted(), "component is not mounted");
        unmount();
        this.parent = null;
        this.offset = -1;
    }

}
