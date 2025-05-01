package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class SpriteNode extends Node {

    private String src = null;
    private BadSprite sprite = null;
    private BadSprite hoverSprite = null;

    private int x = 0;
    private int y = 0;

    public SpriteNode() {
        super("sprite");
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean updated = super.updateFromProps(props);

        var oldSrc = this.src;
        if (props.hasMember("src")) {
            this.src = props.getMember("src").asString();
            if (!this.src.equals(oldSrc)) {
                this.sprite = BadSprite.require(this.src);
                this.hoverSprite = BadSprite.SPRITE_MAP.get(this.src + "_hover");
                updated = true;
            }
        } else {
            this.src = null;
            this.sprite = null;
            this.hoverSprite = null;
            updated |= oldSrc != null;
        }

        int oldX = this.x, oldY = this.y;
        if (props.hasMember("x")) {
            this.x = props.getMember("x").asInt();
        } else this.x = 0;
        if (props.hasMember("y")) {
            this.y = props.getMember("y").asInt();
        } else this.y = 0;
        updated |= oldX != this.x || oldY != this.y;

        return updated;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        if (sprite == null) return;

        builder.draw(this.x, this.y, sprite);

        if (hoverSprite != null) {
            var withHoverIcon = Component.text(hoverSprite.fontChar())
                    .color(FontUtil.computeShadowPos(FontUtil.Size.S3X3, builder.absoluteX(), builder.absoluteY()))
                    .shadowColor(ShadowColor.none())
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(FontUtil.computeOffset(-hoverSprite.width() - 1)));
            builder.editSlots(0, 0, builder.availWidth(), builder.availHeight(), DataComponents.CUSTOM_NAME, (Function<Component, Component>)
                    old -> withHoverIcon.append(Objects.requireNonNullElse(old, Component.empty())));
        }
    }
}
