package net.hollowcube.canvas.internal.standalone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*

Properties of element tree
- context is passed down to all children in constructor, meaning views can init their fields immediately
- all elements are clonable, meaning they can be reused


 */

/**
 * ViewContainer is the {@link net.hollowcube.canvas.Element} which is represented by a {@link net.hollowcube.canvas.View}.
 */
public class ViewContainer extends BoxContainer {

    public ViewContainer(@Nullable String id, int width, int height, @NotNull Align align) {
        super(id, width, height, align);
    }

    protected ViewContainer(@NotNull ViewContainer other) {
        super(other);
    }

    @Override
    public @NotNull ViewContainer dup() {
        return new ViewContainer(this);
    }

}
