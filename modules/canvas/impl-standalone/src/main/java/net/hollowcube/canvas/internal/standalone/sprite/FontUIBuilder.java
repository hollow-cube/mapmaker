package net.hollowcube.canvas.internal.standalone.sprite;

import net.hollowcube.common.util.FontUtil;
import org.jetbrains.annotations.NotNull;

public class FontUIBuilder {
    private final StringBuilder builder = new StringBuilder();
    private int pos = 0;

    /**
     * Draws the given sprite with an offset from the given slot
     * @param sprite
     * @param slot
     * @return
     */
    public @NotNull FontUIBuilder draw(@NotNull Sprite sprite, int slot) {
        int offset = sprite.offsetX() + (slot * 18); // absolute offset of this char
        builder.append(FontUtil.computeOffset(offset - pos))
                .append(sprite.fontChar());
        pos = offset + sprite.width() + 1;
        return this;
    }

    /** Moves to this absolute position */
    public void pos(int pos) {
        builder.append(FontUtil.computeOffset(pos - this.pos));
        this.pos = pos;
    }

    public void append(@NotNull String rawText) {
        append(rawText, FontUtil.measureText(rawText));
    }

    public void append(@NotNull String rawText, int length) {
        builder.append(rawText);
        this.pos += length;
    }

    public @NotNull String build() {
        return builder.toString();
    }
}
