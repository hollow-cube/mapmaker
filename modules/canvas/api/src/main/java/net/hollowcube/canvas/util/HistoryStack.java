package net.hollowcube.canvas.util;

import net.hollowcube.canvas.View;
import org.jetbrains.annotations.NotNull;

/**
 * A history stack is a stack of {@link View}s which can be navigated forward and back.
 */
public interface HistoryStack {

    /**
     * Pushes a view onto the stack, and shows it to the viewer.
     *
     * @param view the view to push
     */
    void push(@NotNull View view);

    /**
     * Works the same way as {@link #push(View)}, but the view will not be added permanently to the stack.
     * <p>
     * For example, if we start with an empty stack, open View A, then push View B transiently,
     * then push view C, then pop. The resulting visible view will be View A.
     *
     * @param view the view to push
     *
     * @see #push(View)
     */
    void pushTransient(@NotNull View view);


    /**
     * Returns true if there are no more views on the stack.
     * <p>
     * If true, {@link #pop()} will always return false.
     */
    boolean isEmpty();

    /**
     * Navigates back to the previous view on the stack, if there is one.
     *
     * @return true if the stack was popped, false if it was empty.
     */
    boolean pop();
}
