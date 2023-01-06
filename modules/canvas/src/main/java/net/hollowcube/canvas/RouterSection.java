package net.hollowcube.canvas;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        this(section, context, null);
    }

    public RouterSection(@NotNull Section section, @NotNull Map<Class<?>, Object> context, @Nullable Player player) {
        super(section, context, player);
        history.addLast(section);
        current = section;
    }

    // Public API


    @Override
    public <T> @NotNull T getContext(@NotNull Class<T> type) {
        return super.getContext(type);
    }

    public boolean hasHistory() {
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

    /** Pushes a new view onto the inventory, clearing the history stack */
    public void pushNew(@NotNull Section section) {
        history.clear();
        push(section);
    }

    public void pop() {
        if (!hasHistory()) {
            // If there is no history close the GUI
            getInventory().getViewers().forEach(Player::closeInventory);
            return;
        }

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
