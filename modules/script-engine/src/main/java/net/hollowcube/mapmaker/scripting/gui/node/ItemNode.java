package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.component.DataComponents;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public class ItemNode extends Node {

    private String model = null;

    public ItemNode() {
        super("item");

        slotWidth = 1;
        slotHeight = 1;
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean changed = super.updateFromProps(props);

        final String oldModel = this.model;
        if (props.hasMember("model")) {
            final String rawModel = props.getMember("model").asString();
            final BadSprite sprite = BadSprite.SPRITE_MAP.get(rawModel);
            if (sprite != null) {

            }
        } else this.model = null;
        changed |= (oldModel != null && this.model == null)
                || (oldModel == null && this.model != null)
                || (oldModel != null && !oldModel.equals(this.model));

        if (slotWidth < 1 || slotHeight < 1) {
            throw new IllegalArgumentException("Slot width and height must be at least 1");
        }

        return changed;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.ITEM_MODEL, "minecraft:stone");
    }
}
