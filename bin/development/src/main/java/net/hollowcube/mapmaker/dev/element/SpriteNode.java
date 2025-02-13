package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.dev.element.PropHelper.getInt;
import static net.hollowcube.mapmaker.dev.element.PropHelper.getString;

public class SpriteNode extends Node {

    private String src;
    private BadSprite sprite;

    private int xOffset = 0;
    private int yOffset = 0;

    @Override
    public @NotNull SpriteNode readProps(@NotNull Value props, @NotNull Value[] children) {
        super.readProps(props, children);

        var oldSrc = this.src;
        this.src = getString(props, "src");
        this.sprite = oldSrc == null || !oldSrc.equals(this.src) ? BadSprite.require(this.src) : null;

        this.xOffset = getInt(props, "x", 0);
        this.yOffset = getInt(props, "y", 0);

        return this;
    }

    @Override
    public void render(@NotNull RenderContext context) {
        context.drawSprite(sprite, xOffset, yOffset);
    }

    @Override
    public String toString() {
        return "SpriteNode{" +
                "src='" + src + '\'' +
                ", sprite=" + sprite +
                '}';
    }
}
