package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MessagesBase extends ComponentLike {

    String translationKey();

    default Component with(Object... args) {
        return Component.translatable(translationKey(), asArgs(args));
    }

    default Component with(List<Component> args) {
        return Component.translatable(translationKey(), args);
    }

    default Component asError(@Nullable String traceId, Object... args) {
        var base = Component.text("■ ", TextColor.color(0xFF2D2D));
        if (traceId != null) {
            base = base.clickEvent(ClickEvent.copyToClipboard(traceId))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy trace")));
        }
        return Component.textOfChildren(base, with(args));
    }

    @Override
    default Component asComponent() {
        return Component.translatable(translationKey());
    }

    static List<ComponentLike> asArgs(Object... args) {
        if (args.length == 0) return List.of();
        var componentArgs = new ComponentLike[args.length];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            componentArgs[i] = switch (arg) {
                case Component comp -> comp;
                case Number number -> TranslationArgument.numeric(number);
                case Boolean bool -> TranslationArgument.bool(bool);
                default -> Component.text(arg.toString());
            };
        }
        return List.of(componentArgs);
    }
}
