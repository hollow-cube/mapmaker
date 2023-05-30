package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextElement extends BaseElement implements Text {

    private final String font;
    private final int shift;

    private String text = "Map Name";

    public TextElement(@NotNull ElementContext context, @Nullable String id, int width, int height, @NotNull String font, int shift) {
        super(context, id, width, height);
        this.font = font;
        this.shift = shift;
    }

    protected TextElement(@NotNull ElementContext context, @NotNull TextElement other) {
        super(context, other);
        this.font = other.font;
        this.shift = other.shift;
    }

    @Override
    public void setText(@NotNull String text) {
        this.text = text;
    }

    @Override
    public void buildTitle(@NotNull FontUIBuilder sb, int x, int y) {
        if (shouldDelegateDraw()) return;

        //todo we should only rewrite and measure the text once, not every time we draw it
        sb.pos(shift);
        // Note that we provide the length, because it needs to be the width of the text before being rewritten
        //todo why is this const offset required? seems like the measurements of the ascii font are different from the measurements of the offset ascii one.
        sb.append(FontUtil.rewrite(font, text), FontUtil.measureText(text) - 3);

    }

    @Override
    public @NotNull BaseElement clone(@NotNull ElementContext context) {
        return new TextElement(context, this);
    }
}
