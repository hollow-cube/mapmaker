package net.hollowcube.command.suggestion;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public record SuggestionEntry(
        String replacement,
        @Nullable Component tooltip
) {
}
