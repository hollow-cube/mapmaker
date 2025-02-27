package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.minestom.server.item.ItemComponent;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public class ItemNode extends Node {
    public ItemNode() {
        super("item");

        slotWidth = 1;
        slotHeight = 1;
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean changed = super.updateFromProps(props);

        if (slotWidth < 1 || slotHeight < 1) {
            throw new IllegalArgumentException("Slot width and height must be at least 1");
        }

        return changed;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        builder.editSlots(0, 0, slotWidth, slotHeight, ItemComponent.ITEM_MODEL, "minecraft:stone");
    }
}
