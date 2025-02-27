package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public class TooltipNode extends GroupNode {

    private String translationKey = "missing";

    public TooltipNode() {
        super("tooltip");
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean changed = super.updateFromProps(props);

        if (props.hasMember("translationKey")) {
            this.translationKey = props.getMember("translationKey").asString();
            changed = true; // todo make translation component or whatever
        }

        return changed;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        super.build(builder); // GroupNode for layout

        builder.editSlotsWithout(0, 0, width(), height(), ItemComponent.HIDE_TOOLTIP);
        builder.editSlots(0, 0, width(), height(), ItemComponent.CUSTOM_NAME, Component.text(this.translationKey));
    }
}
