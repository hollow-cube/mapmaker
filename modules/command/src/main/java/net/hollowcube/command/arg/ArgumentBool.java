package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public class ArgumentBool extends Argument<Boolean> {
    // This is case sensitive right now, which matches Brigadier, maybe should have an option to match case insensitively for server side only?

    public ArgumentBool(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Boolean> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.ALPHANUMERIC);
        if ("true".equals(word)) return success(true);
        if ("false".equals(word)) return success(false);
        if ("true".startsWith(word) || "false".startsWith(word)) return partial();
        return syntaxError();
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        if ("true".startsWith(raw)) {
            suggestion.add("true");
        } else if ("false".startsWith(raw)) {
            suggestion.add("false");
        }
    }

    @Override
    public String toString() {
        return "bool@" + id();
    }

    @Override
    public void properties(@NotNull NetworkBuffer buffer) {} // boolean is boring :3

    @Override
    public @NotNull ArgumentParserType argumentType() {
        return ArgumentParserType.BOOL;
    }
}
