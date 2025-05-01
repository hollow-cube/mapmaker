package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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

        builder.editSlotsWithout(0, 0, width(), height(), DataComponents.TOOLTIP_DISPLAY);
        builder.editSlots(0, 0, width(), height(), DataComponents.CUSTOM_NAME, (Function<Component, Component>) old ->
                Objects.requireNonNullElse(old, Component.empty()).append(Component.translatable(this.translationKey + ".name")));
        builder.editSlots(0, 0, width(), height(), DataComponents.LORE, LanguageProviderV2.translateMulti(this.translationKey + ".lore", List.of()));
    }
}
