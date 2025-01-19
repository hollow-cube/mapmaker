package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpriteElement extends LabelElement {

    public SpriteElement(@NotNull ElementContext context, @Nullable String id) {
        super(context, id, 0, 0, "");
    }

    protected SpriteElement(@NotNull ElementContext context, @NotNull SpriteElement other) {
        super(context, other);
    }

    @Override
    public void buildTitle(@NotNull FontUIBuilder sb, int x, int y) {
        // Always draw sprite elements relative to the top left corner
        this.drawBackgroundSprite(sb, 0, 0);
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new SpriteElement(context, this);
    }
}
