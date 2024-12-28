package net.hollowcube.canvas.internal.standalone.sprite;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.color.AlphaColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FontUIBuilder {
    //todo probably i need to optimize this, since its called fairly often.
    //     the first thing i can think of is to use a combination of stringbuilder and textcomponent builder,
    //     only using textcomponent builder when there is a color change.
    //     secondly, probably i need to look into incremental recreation of the ui, patching the text instead of rebuilding it from scratch.
//    private List<Component> children = new ArrayList<>();
    private TextComponent.Builder builder = Component.text().color(NamedTextColor.WHITE);
//    private final StringBuilder builder = new StringBuilder();

    private boolean withShadow = false;
    private TextColor color;
    private int pos = 0;

    /**
     * Draws the given sprite with an offset from the given slot
     *
     * @param sprite The sprite to draw
     * @param slot   The slot to draw it at
     * @return This object, mutated by the parameters
     */
    public @NotNull FontUIBuilder draw(@NotNull Sprite sprite, int slot) {
        return draw(sprite, slot, null);
    }

    /**
     * Draws the given sprite with an offset from the given slot
     *
     * @param sprite The sprite to draw
     * @param slot   The slot to draw it at
     * @return This object, mutated by the parameters
     */
    public @NotNull FontUIBuilder draw(@NotNull Sprite sprite, int slot, @Nullable TextColor color) {
        int offset = sprite.offsetX() + (slot * 18); // absolute offset of this char
        appendRaw(FontUtil.computeOffset(offset - pos));
        if (color != null) this.color(color);
        appendRaw(String.valueOf(sprite.fontChar()));
        pos = offset + sprite.width() + 1 - sprite.rightOffset();
        return this;
    }

    /**
     * Moves to this absolute position
     */
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

    public void withShadow(boolean withShadow) {
        this.withShadow = withShadow;
    }

    public @NotNull Component build() {
//        return Component.textOfChildren(children.toArray(new ComponentLike[0]));
        return builder.build();
    }

    private void appendRaw(@NotNull String text) {
        var component = Component.text(text, color);
        if (withShadow) {
            // By default minecraft scales the text color by 0.25 to create the shadow. we do the same here.
            component = component.shadowColor(new AlphaColor(
                    255,
                    (int) (this.color.red() * 0.25f),
                    (int) (this.color.green() * 0.25f),
                    (int) (this.color.blue() * 0.25f)));
        }

        builder.append(component);
        this.color = NamedTextColor.WHITE;
        this.withShadow = false;
    }
}
