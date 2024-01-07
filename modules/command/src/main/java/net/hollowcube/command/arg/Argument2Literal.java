package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class Argument2Literal extends Argument2<String> {
    private final String literal;

    public Argument2Literal(@NotNull String literal) {
        super(literal);
        this.literal = literal;
    }

    @Override
    public @NotNull ParseResult2<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var compareValue = literal.toLowerCase(Locale.ROOT);

        int pos = reader.pos();
        var word = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
        if (compareValue.equals(word)) return success(literal);
        else if (compareValue.startsWith(word)) return partial();
        else return syntaxError(pos);
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        if (literal.toLowerCase(Locale.ROOT).startsWith(raw.toLowerCase(Locale.ROOT))) suggestion.add(literal);
    }
}
