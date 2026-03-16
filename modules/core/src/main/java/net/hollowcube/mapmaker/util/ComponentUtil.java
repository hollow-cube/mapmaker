package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

public final class ComponentUtil {

    public static Component createBasicCopy(String text) {
        return createBasicCopy(text, text);
    }

    public static Component createBasicCopy(String text, String copy) {
        return Component.text(text)
                .hoverEvent(Component.text("Click to copy"))
                .clickEvent(ClickEvent.copyToClipboard(copy));
    }
}
