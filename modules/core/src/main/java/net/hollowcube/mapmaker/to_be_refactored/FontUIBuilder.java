package net.hollowcube.mapmaker.to_be_refactored;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FontUIBuilder {
    //todo probably i need to optimize this, since its called fairly often.
    //     the first thing i can think of is to use a combination of stringbuilder and textcomponent builder,
    //     only using textcomponent builder when there is a color change.
    //     secondly, probably i need to look into incremental recreation of the ui, patching the text instead of rebuilding it from scratch.
//    private List<Component> children = new ArrayList<>();
    private TextComponent.Builder builder = Component.text().color(NamedTextColor.WHITE);
//    private final StringBuilder builder = new StringBuilder();

    private final List<TextColor> colorStack = new ArrayList<>();
    private int pos = 0;

    /**
     * Draws the given sprite with an offset from the given slot
     *
     * @param sprite The sprite to draw
     * @param slot   The slot to draw it at
     * @return This object, mutated by the parameters
     */
    public @NotNull FontUIBuilder draw(@NotNull BadSprite sprite, int slot) {
        int offset = sprite.offsetX() + (slot * 18); // absolute offset of this char
        appendRaw(FontUtil.computeOffset(offset - pos));
        appendRaw(String.valueOf(sprite.fontChar()));
        pos = offset + sprite.width() + 1 - sprite.rightOffset();
        return this;
    }

    public @NotNull FontUIBuilder drawInPlace(@NotNull BadSprite sprite) {
        appendRaw(FontUtil.computeOffset(sprite.offsetX()));
        appendRaw(String.valueOf(sprite.fontChar()));
        pos += sprite.offsetX() + sprite.width() + 1 - sprite.rightOffset();
        return this;
    }

    /**
     * Moves to this absolute position
     */
    public @NotNull FontUIBuilder pos(int pos) {
        appendRaw(FontUtil.computeOffset(pos - this.pos));
        this.pos = pos;
        return this;
    }

    public @NotNull FontUIBuilder offset(int amount) {
        appendRaw(FontUtil.computeOffset(amount));
        this.pos += amount;
        return this;
    }

    public void append(@NotNull String rawText) {
        append(rawText, FontUtil.measureText(rawText));
    }

    public void append(@NotNull String rawText, int length) {
        appendRaw(rawText);
        this.pos += length;
    }

    public void append(@NotNull String font, @NotNull String rawText) {
        append(FontUtil.rewrite(font, rawText), FontUtil.measureText(font, rawText));
    }

    public @NotNull FontUIBuilder pushColor(@NotNull TextColor color) {
        this.colorStack.add(color);
        return this;
    }

    public @NotNull FontUIBuilder popColor() {
        this.colorStack.remove(this.colorStack.size() - 1);
        return this;
    }

    public int mark() {
        return colorStack.size();
    }

    public void restore(int mark) {
        while (colorStack.size() > mark) {
            popColor();
        }
    }

    public @NotNull Component build() {
        return build(false);
    }


    public void tempReset() {
        appendRaw(FontUtil.computeOffset(-pos));
        pos = 0;
    }

    public @NotNull Component build(boolean reset) {
        if (reset) pos(0);
        return builder.build();
    }

    private void appendRaw(@NotNull String text) {
        var color = colorStack.isEmpty() ? NamedTextColor.WHITE : colorStack.get(colorStack.size() - 1);
        builder.append(Component.text(text, color));
    }
}
