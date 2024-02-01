package net.hollowcube.terraform.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public interface MessageSet extends ComponentLike {

    @NotNull String key();

    default @NotNull Component with(@NotNull Object... args) {
        var formatted = new ArrayList<ComponentLike>();
        for (var arg : args) {
            if (arg instanceof ComponentLike comp) {
                formatted.add(comp);
            } else {
                formatted.add(Component.text(String.valueOf(arg)));
            }
        }
        return Component.translatable(key(), formatted);
    }

    @Override
    default @NotNull Component asComponent() {
        return with();
    }

}
