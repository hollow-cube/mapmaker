package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
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

    public TextNode() {
        super("text");
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

        builder.drawText(0, 0, text);
    }
}
