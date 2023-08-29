package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface InsertingWithArgs {
    @NotNull Component value(@NotNull List<Component> args);
}
