package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextElement extends ButtonElement implements Text {

    private final String font;
    private final int shift;
    private final boolean centered;
    private final String initialText;

    private String text = "";
    private TextColor color = NamedTextColor.WHITE;

    public TextElement(@NotNull ElementContext context, @Nullable String id, int width, int height,
                       @NotNull String translationKey, @NotNull String font, int shift, boolean centered,
                       @NotNull String initialText) {
        super(context, id, width, height, translationKey);
        this.font = font;
        this.shift = shift;
        this.centered = centered;
        this.initialText = this.text = initialText;
    }

    protected TextElement(@NotNull ElementContext context, @NotNull TextElement other) {
        super(context, other);
        this.font = other.font;
        this.shift = other.shift;
        this.centered = other.centered;
        this.initialText = this.text = other.initialText;
    }

    @Override
    public void setText(@NotNull String text, @NotNull TextColor color) {
        this.text = text;
        this.color = color;
        context.markDirty();
    }

    @Override
    public void buildTitle(@NotNull FontUIBuilder sb, int x, int y) {
        if (shouldDelegateDraw()) return;
        super.buildTitle(sb, x, y);

        //todo we should only rewrite and measure the text once, not every time we draw it
        var textWidth = FontUtil.measureText(text);

        if (centered) {
            int totalWidth = width() * 18 - 2; // take off 2 for the left and right padding
            sb.pos(shift + ((totalWidth - textWidth) / 2) + (x * 18));
        } else {
            sb.pos(shift + (x * 16));
        }

        sb.color(color);
        // Note that we provide the length, because it needs to be the width of the text before being rewritten
        sb.append(FontUtil.rewrite(font, text), textWidth);
    }

    @Override
    public @NotNull TextElement clone(@NotNull ElementContext context) {
        return new TextElement(context, this);
    }
}
