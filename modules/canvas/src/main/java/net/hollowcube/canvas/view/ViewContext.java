package net.hollowcube.canvas.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public sealed interface ViewContext permits ViewContextImpl {

    /**
     * The most basic primitive of controlling view updates. Marks the view as dirty, causing it to be re-rendered.
     * This method should be used carefully, and must never be called by a direct path from a view function.
     * <p>
     * How the view is re-rendered is implementation dependent, it may trigger immediately or be deferred, it may
     * re-render the entire ui or only the view that was marked dirty, etc.
     * <p>
     * {@link #set(String, Object)} triggers a view update on its own, this method does not need to be called in addition.
     */
    void markDirty();


    <T> @NotNull T get(@NotNull String name, @NotNull Supplier<T> def);
    <T> @NotNull T get(@NotNull String name, @NotNull T def);
    default <T> @Nullable T get(@NotNull String name) {
        return get(name, () -> null);
    }

    <T> void set(@NotNull String name, @NotNull T value);

    //todo not sure i want to keep this flag stuff. its basically just sugar for handling a single bit of state.
    long flag();
    long flag(long flag);

    /**
     * Creates a child context with the given component. Child contexts are used to preserve state
     * correctly between renders.
     * <p>
     * A child context <i>must</i> be created every time it is passed to a child. You may not pass
     * the same context to multiple {@link View}s. The given ID must be unique to each child, and
     * will be preserved between renders. Some common cases are found below:
     * <ul>
     *     <li>When rendering a list, each one must have a unique ID, taking into account both its index & some ID.</li>
     * </ul>
     */
    @NotNull View create(@NotNull String id, @NotNull ViewFunc viewFunc);


    // Router implementation

    /** Returns true if there is a history (past the current page), false otherwise */
    boolean hasHistory();

    /**
     * Pushes a view onto the history stack. This will replace the current view, as if a new inventory was opened.
     *
     * @param view The view to push onto the history stack. Must have a width of 9.
     */
    void pushView(int width, int height, @NotNull ViewFunc view);

    /**
     * Pops the current view off the history stack and returns to the previous one, or closes the inventory if
     * {@link #hasHistory()} is false (there is no history).
     */
    void popView();

}
