package net.hollowcube.canvas.internal.standalone.sprite;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
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

    private TextColor color;
    private int pos = 0;

    /**
     * Draws the given sprite with an offset from the given slot
     * @param sprite
     * @param slot
     * @return
     */
    public @NotNull FontUIBuilder draw(@NotNull Sprite sprite, int slot) {
        int offset = sprite.offsetX() + (slot * 18); // absolute offset of this char
        appendRaw(FontUtil.computeOffset(offset - pos));
        appendRaw(String.valueOf(sprite.fontChar()));
        pos = offset + sprite.width() + 1;
        return this;
    }

    /** Moves to this absolute position */
    public void pos(int pos) {
        appendRaw(FontUtil.computeOffset(pos - this.pos));
        this.pos = pos;
    }

    public void append(@NotNull String rawText) {
        append(rawText, FontUtil.measureText(rawText));
    }

    public void append(@NotNull String rawText, int length) {
        appendRaw(rawText);
        this.pos += length;
    }

    public void color(@NotNull TextColor color) {
        this.color = color;
    }

    public @NotNull Component build() {
//        return Component.textOfChildren(children.toArray(new ComponentLike[0]));
        return builder.build();
    }

    private void appendRaw(@NotNull String text) {
//        children.add(Component.text(text).color(color));
        builder.append(Component.text(text, color));
        this.color = NamedTextColor.WHITE;
    }
}
