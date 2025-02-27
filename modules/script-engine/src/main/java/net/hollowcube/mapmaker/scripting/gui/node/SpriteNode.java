package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public class SpriteNode extends Node {

    // TODO perhaps could handle prop types automagically with some api like this:
    //  this would make it easier to auto generate so seems relevant.
//    private final Prop<BadSprite> src = stringProp("src").map(BadSprite::require);

    private BadSprite srcSprite = null;

    private int x = 0;
    private int y = 0;

    public SpriteNode() {
        super("sprite");
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean updated = super.updateFromProps(props);

//        var oldSrc = this.src;
//        this.src = getString(props, "src");
//        this.sprite = oldSrc == null || !oldSrc.equals(this.src) ? BadSprite.require(this.src) : null;
        this.srcSprite = BadSprite.require(props.getMember("src").asString());
        updated = true;

        if (props.hasMember("x")) {
            this.x = props.getMember("x").asInt();
        } else this.x = 0;
        if (props.hasMember("y")) {
            this.y = props.getMember("y").asInt();
        } else this.y = 0;

        return updated;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        if (srcSprite == null) return;

        builder.draw(this.x, this.y, srcSprite);

//        builder.editSlots(0, 0, 1, 1, ItemComponent.ITEM_MODEL, "minecraft:crafting_table");
    }
}
