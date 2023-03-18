package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

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

    @Override
    default @NotNull Component asComponent() {
        return Component.translatable(translationKey());
    }
}
