package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PaginationElement extends ContainerElement {
    private final Class<? extends View> itemClass;

    public PaginationElement(@NotNull ElementContext context, @Nullable String id, int width, int height, Class<? extends View> itemClass) {
        super(context, id, width, height);
        this.itemClass = itemClass;
    }

    protected PaginationElement(@NotNull ElementContext context, @NotNull PaginationElement other) {
        super(context, other);
        this.itemClass = other.itemClass;
    }

    @Override
    public @NotNull PaginationElement clone(@NotNull ElementContext context) {
        return new PaginationElement(context, this);
    }
}
