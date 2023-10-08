package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArgumentGreedyString extends Argument<String> {

    ArgumentGreedyString(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return new ParseSuccess<>(reader.readRemaining());
    }

    @Override
    public @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull StringReader reader) {
        // Never give suggestions, a map argument using this can handle suggestions.
        return new SuggestionResult.Success(0, 0, List.of());
    }
}
