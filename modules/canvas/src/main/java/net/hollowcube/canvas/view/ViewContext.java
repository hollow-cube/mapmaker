package net.hollowcube.canvas.view;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public sealed interface ViewContext permits ViewContextImpl {

    <T> @NotNull T get(@NotNull String name, @NotNull Supplier<T> def);
    <T> @NotNull T get(@NotNull String name, @NotNull T def);

    <T> void set(@NotNull String name, @NotNull T value);

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

}
