package net.hollowcube.mapmaker.panels;

import net.hollowcube.common.util.FontUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Text extends Button {
    private static final int MAX_NUMBER = 1 << 29;

    public static final int START = 0;
    public static final int CENTER = 1 << 30;
    public static final int END = 1 << 31;

    private String text;
    private int xAlign = START;
    private int yAlign = START;

    public Text(@NotNull String translationKey, int slotWidth, int slotHeight, @NotNull String text) {
        super(translationKey, slotWidth, slotHeight);
        this.text = text;
    }

    public @NotNull Text text(@NotNull String text) {
        this.text = text;
        if (host != null) host.queueRedraw();
        return this;
    }

    public @NotNull Text align(int x, int y) {
        this.xAlign = x;
        this.yAlign = y;
        if (host != null) host.queueRedraw();
        return this;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        super.build(builder);

        if (text.isEmpty()) return;
        int x = computeAlignment(this.xAlign, FontUtil.measureText(text), builder.availWidth() * 18);
        int y = computeAlignment(this.yAlign, FontUtil.DEFAULT_HEIGHT, builder.availHeight() * 18);
        builder.drawText(x, y, text);
    }

    private int computeAlignment(int value, int size, int parentSize) {
        if (value > -MAX_NUMBER && value < MAX_NUMBER) {
            return value; // Start is 0 so also covered here
        }

        if (value == CENTER) {
            return (parentSize - size) / 2;
        } else if (value == END) {
            return parentSize - size;
        } else {
            return value;
        }
    }


    // DSL overrides

    @Override
    public @NotNull Text background(@Nullable String sprite) {
        return background(sprite, 0, 0);
    }

    @Override
    public @NotNull Text background(@Nullable String sprite, int x, int y) {
        super.background(sprite, x, y);
        return this;
    }
}
