package net.hollowcube.common.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class ExtraComponents {

    public static TranslatableBuilder translatable(String key) {
        return new TranslatableBuilder(key);
    }

    public static Component multiline(List<Component> components) {
        return Component.join(JoinConfiguration.newlines(), components);
    }

    public static Component noItalic(ComponentLike like) {
        return like.asComponent().style(style -> style.decoration(TextDecoration.ITALIC, false));
    }
}
