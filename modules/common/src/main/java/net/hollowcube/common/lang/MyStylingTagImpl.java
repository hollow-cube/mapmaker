package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.internal.parser.node.ElementNode;
import net.kyori.adventure.text.minimessage.tag.Tag;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class MyStylingTagImpl implements Tag, InsertingWithArgs {
    private final ElementNode partial;

    MyStylingTagImpl(ElementNode partial) {
        this.partial = partial;
    }

    @Override
    public Component value(List<Component> args) {
        var hoverComponent = LanguageProviderV2.treeToComponent(this.partial, args);
        return Component.text("", Style.style(HoverEvent.showText(hoverComponent)));
    }
}
