package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import org.jetbrains.annotations.NotNull;

public class SpacerElement extends BaseElement implements SpriteHolder {

    public SpacerElement(@NotNull ElementContext context, int width, int height) {
        super(context, null, width, height);
    }

    protected SpacerElement(@NotNull ElementContext context, @NotNull SpacerElement other) {
        super(context, other);
    }

    @Override
    public @NotNull BaseElement clone(@NotNull ElementContext context) {
        return new SpacerElement(context, this);
    }
}
