package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;

public final class ComponentUtil {

    public static @NotNull Component createBasicCopy(@NotNull String text) {
        return createBasicCopy(text, text);
    }

    public static @NotNull Component createBasicCopy(@NotNull String text, @NotNull String copy) {
        return Component.text(text)
                .hoverEvent(Component.text("Click to copy"))
                .clickEvent(ClickEvent.copyToClipboard(copy));
    }
}
