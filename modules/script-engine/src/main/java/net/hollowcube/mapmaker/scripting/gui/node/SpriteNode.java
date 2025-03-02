package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public class SpriteNode extends Node {

    private String src = null;
    private BadSprite sprite = null;

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
                updated = true;
            }
        } else {
            this.src = null;
            this.sprite = null;
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
    }
}
