package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.hollowcube.mapmaker.scripting.gui.util.Align;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public class TextNode extends GroupNode {
    public static class Raw extends Node {
        private String content;

        public Raw(@NotNull String initialContent) {
            super("text-raw");
            this.content = initialContent;
        }

        public void setContent(@NotNull String content) {
            this.content = content;
        }

        @Override
        public void build(@NotNull MenuBuilder builder) {
            throw new UnsupportedOperationException("Raw text nodes cannot be built");
        }
    }

    private final Align x = new Align();
    private final Align y = new Align();

    public TextNode() {
        super("text");
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean changed = super.updateFromProps(props);

        changed |= this.x.updateFromProps(props, "x");
        changed |= this.y.updateFromProps(props, "y");

        return changed;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        var string = new StringBuilder();
        for (var child : children) {
            if (child instanceof Raw) {
                string.append(((Raw) child).content);
            }
        }
        var text = string.toString();
        if (text.isEmpty()) return;

        int x = this.x.value(FontUtil.measureText(text), builder.availWidth() * 18);
        int y = this.y.value(FontUtil.DEFAULT_HEIGHT, builder.availHeight() * 18);
        builder.drawText(x, y, text);
    }

}
