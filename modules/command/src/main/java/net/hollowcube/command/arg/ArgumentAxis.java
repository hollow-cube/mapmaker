package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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
            else return syntaxError("Invalid axis '%c' for %s".formatted(c, this.id()));
        }
        return success(new Result(x, y, z));
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        var word = raw.toLowerCase(Locale.ROOT);
        if (!word.contains("x")) suggestion.add("x");
        if (!word.contains("y")) suggestion.add("y");
        if (!word.contains("z")) suggestion.add("z");
    }

    @Override
    public String toString() {
        return "axis@" + id();
    }
}
