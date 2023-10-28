package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

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
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        var pos = reader.pos();
        var word = reader.readWord(WordType.ALPHANUMERIC);
        if ("true".startsWith(word)) {
            suggestion.add("true");
        } else if ("false".startsWith(word)) {
            suggestion.add("false");
        }
    }

    @Override
    public String toString() {
        return "bool@" + id();
    }
}
