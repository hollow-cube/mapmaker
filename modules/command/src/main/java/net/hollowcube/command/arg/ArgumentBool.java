package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class ArgumentBool extends Argument<Boolean> {
    // This is case sensitive right now, which matches Brigadier, maybe should have an option to match case insensitively for server side only?

    public ArgumentBool(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Boolean> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.ALPHANUMERIC);
        if ("true".equals(word)) return new ParseSuccess<>(true);
        if ("false".equals(word)) return new ParseSuccess<>(false);
        if ("true".startsWith(word) || "false".startsWith(word)) return new ParsePartial<>();
        return new ParseFailure<>();
    }

    @Override
    public @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var pos = reader.pos();
        var word = reader.readWord(WordType.ALPHANUMERIC);
        if ("true".startsWith(word)) {
            return new SuggestionResult.Success(pos, word.length(), List.of(new SuggestionEntry("true", null)));
        } else if ("false".startsWith(word)) {
            return new SuggestionResult.Success(pos, word.length(), List.of(new SuggestionEntry("false", null)));
        } else {
            return new SuggestionResult.Failure();
        }
    }

    @Override
    public String toString() {
        return "bool@" + id();
    }
}
