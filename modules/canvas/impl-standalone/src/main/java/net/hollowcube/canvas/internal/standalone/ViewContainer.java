package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * ViewContainer is the {@link net.hollowcube.canvas.Element} which is represented by a {@link net.hollowcube.canvas.View}.
 */
public class ViewContainer extends BoxContainer {

    private View associatedView;

    public ViewContainer(@NotNull ElementContext context, @Nullable String id, int width, int height, @NotNull Align align) {
        super(context, id, width, height, align);
        this.associatedView = null;
    }

    protected ViewContainer(@NotNull ElementContext context, @NotNull ViewContainer other) {
        super(context, other);
        this.associatedView = null;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public @NotNull View getAssociatedView() {
        return Objects.requireNonNull(associatedView,
                "ViewContainer has no associated view. Something went wrong...");
    }

    public void setAssociatedView(@NotNull View associatedView) {
        this.associatedView = associatedView;
    }

    @Override
    public @NotNull ViewContainer clone(@NotNull ElementContext context) {
        if (associatedView == null)
            return new ViewContainer(context, this);

        // This is pretty yikes, but the problem is that when we duplicate a view, the associatedView is reset, so we need to instantiate a new one.

        try {
            var constructor = associatedView.getClass().getConstructor(Context.class);
            constructor.setAccessible(true);
            var other = ((ViewContainer) constructor.newInstance(context).element());
            other.setId(id);
            return other;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

}
