package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ArgumentAxis extends Argument<ArgumentAxis.Result> {

    public record Result(boolean x, boolean y, boolean z) {
    }


    public ArgumentAxis(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Result> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.ALPHANUMERIC);
        boolean x = false, y = false, z = false;
        for (char c : word.toCharArray()) {
            if (c == 'x') x = true;
            else if (c == 'y') y = true;
            else if (c == 'z') z = true;
            else return new ParseFailure<>();
        }
        return new ParseSuccess<>(new Result(x, y, z));
    }

    @Override
    public @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var pos = reader.pos();
        var word = reader.readWord(WordType.ALPHANUMERIC);

        var suggestions = new ArrayList<SuggestionEntry>();
        if (!word.contains("x")) suggestions.add(new SuggestionEntry("x", null));
        if (!word.contains("y")) suggestions.add(new SuggestionEntry("y", null));
        if (!word.contains("z")) suggestions.add(new SuggestionEntry("z", null));

        return new SuggestionResult.Success(pos + word.length(), 0, suggestions);
    }

    @Override
    public String toString() {
        return "axis@" + id();
    }
}
