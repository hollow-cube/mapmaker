package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;

public final class ComponentUtil {

    public static @NotNull Component createBasicCopy(@NotNull String text) {
        return Component.text(text)
                .hoverEvent(Component.text("Click to copy"))
                .clickEvent(ClickEvent.copyToClipboard(text));
    }
}
