package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.minestom.server.inventory.click.ClickType;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public abstract class Node {
    private final String type; // Name of the node type, mostly used for debug purposes

    private boolean background = false;

    protected int slotWidth = 0;
    protected int slotHeight = 0;

    protected Node(@NotNull String type) {
        this.type = type;
    }

    public @NotNull String type() {
        return this.type;
    }

    public boolean isBackground() {
        return background;
    }

    public int width() {
        return this.slotWidth;
    }

    public int height() {
        return this.slotHeight;
    }

    public boolean updateFromProps(@NotNull Value props) {

        int lastSlotWidth = this.slotWidth;
        if (props.hasMember("slotWidth"))
            this.slotWidth = props.getMember("slotWidth").asInt(); // TODO better error handling
        int lastSlotHeight = this.slotHeight;
        if (props.hasMember("slotHeight"))
            this.slotHeight = props.getMember("slotHeight").asInt(); // TODO better error handling

        background = props.hasMember("position"); // todo obviously wrong

        return lastSlotWidth != this.slotWidth
                || lastSlotHeight != this.slotHeight;
    }

    public abstract void build(@NotNull MenuBuilder builder);

    public boolean handleClick(@NotNull ClickType clickType, int x, int y) {
        return false;
    }

}
