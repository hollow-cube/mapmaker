package net.hollowcube.command.arg;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface SuggestionResult {

    record Success(
            int start,
            int length,
            @NotNull List<SuggestionEntry> suggestions
    ) implements SuggestionResult {
        public Success {
            suggestions = List.copyOf(suggestions);
        }
    }

    record Failure() implements SuggestionResult {
    }
}
