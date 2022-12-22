package net.hollowcube.canvas;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Router component is a replacement for RootComponent, it enables a history/routing feature when used in child components.
 */
public final class RouterSection extends RootSection {
    private final Deque<Section> history = new ArrayDeque<>();
    private Section current;

    public RouterSection(@NotNull Section section) {
        this(section, Map.of());
    }

    public RouterSection(@NotNull Section section, @NotNull Map<Class<?>, Object> context) {
        super(section, context);
        history.addLast(section);
        current = section;
    }

    // Public API

    public boolean hasHistory() {
        // This null check is in case this is called during init somehow.
        // I am not sure how it is happening and need to fix cases where it could.
        if (history == null) return false;
        return history.size() > 1;
    }

    public void push(@NotNull Section section) {
        history.addLast(section);
        replaceInventory(section);
        current = section;
    }

    /** Pushes a new view onto the inventory, but does _not_ save it in the history stack */
    public void pushTransient(@NotNull Section section) {
        replaceInventory(section);
        current = section;
    }

    public void pop() {
        if (!hasHistory()) return;

        history.removeLast();
        replaceInventory(history.getLast());
        current = history.getLast();
    }


    // Implementation detail

    @Override
    protected void mount() {
        super.mount();
        current.setParent(this, 0);
    }

    @Override
    protected void unmount() {
        super.unmount();
        current.removeParent();
    }

}
