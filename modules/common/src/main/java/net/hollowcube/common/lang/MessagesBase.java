package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MessagesBase extends ComponentLike {

    @NotNull String translationKey();

    default @NotNull Component with(@NotNull Object... args) {
        var componentArgs = new Component[args.length];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (arg instanceof Component comp) {
                componentArgs[i] = comp;
            } else {
                componentArgs[i] = Component.text(arg.toString());
            }
        }
        return Component.translatable(translationKey(), componentArgs);
    }

    default @NotNull Component asError(@Nullable String traceId, @NotNull Object... args) {
        var base = Component.text("■ ", TextColor.color(0xFF2D2D));
        if (traceId != null) {
            base = base.clickEvent(ClickEvent.copyToClipboard(traceId))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy trace")));
        }
        return Component.textOfChildren(base, with(args));
    }

    @Override
    default @NotNull Component asComponent() {
        return Component.translatable(translationKey());
    }
}
