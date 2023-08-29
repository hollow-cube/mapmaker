package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.internal.parser.node.ElementNode;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MyStylingTagImpl implements Tag, InsertingWithArgs {
    private final ElementNode partial;

    MyStylingTagImpl(@NotNull ElementNode partial) {
        this.partial = partial;
    }

    @Override
    public @NotNull Component value(@NotNull List<Component> args) {
        var hoverComponent = LanguageProviderV2.treeToComponent(this.partial, args);
        return Component.text("", Style.style(HoverEvent.showText(hoverComponent)));
    }
}
