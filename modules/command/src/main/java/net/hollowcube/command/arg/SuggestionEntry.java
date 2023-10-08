package net.hollowcube.command.arg;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SuggestionEntry(
        @NotNull String replacement,
        @Nullable Component tooltip
) {
}
