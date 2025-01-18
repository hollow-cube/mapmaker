package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;

public class SpriteNode implements Node {
    private final String src;
    private final BadSprite sprite;

    public SpriteNode(@NotNull String src) {
        this.src = src;
        this.sprite = BadSprite.require(src);
    }

    @Override
    public void render(@NotNull RenderContext context) {
        context.drawSprite(sprite, -10, -30);
    }

    @Override
    public String toString() {
        return "SpriteNode{" +
                "src='" + src + '\'' +
                ", sprite=" + sprite +
                '}';
    }
}
