package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ArgumentLiteral extends Argument<String> {
    private final String literal;

    public ArgumentLiteral(@NotNull String literal) {
        super(literal.toLowerCase(Locale.ROOT));
        this.literal = literal.toLowerCase(Locale.ROOT);
    }

    @Override
    public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
        if (literal.equals(word)) return new ParseSuccess<>(literal);
        else if (literal.startsWith(word)) return new ParsePartial<>();
        else return new ParseFailure<>();
    }

    @Override
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        var word = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
        if (literal.startsWith(word)) suggestion.add(literal);
    }
}
