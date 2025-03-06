package net.hollowcube.common.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExtraComponents {

    public static @NotNull TranslatableBuilder translatable(@NotNull String key) {
        return new TranslatableBuilder(key);
    }

    public static @NotNull Component multiline(@NotNull List<Component> components) {
        return Component.join(JoinConfiguration.newlines(), components);
    }
}
