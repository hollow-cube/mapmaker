package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Argument2Word extends Argument2<String> {
    private List<String> values = null;

    Argument2Word(@NotNull String id) {
        super(id);
    }

    public @NotNull Argument2Word with(@NotNull String... values) {
        this.values = List.of(values);
        return this;
    }

    public @NotNull Argument2Word with(@NotNull List<String> values) {
        this.values = List.copyOf(values);
        return this;
    }

    @Override
    public @NotNull ParseResult2<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var start = reader.pos();
        var word = reader.readWord(WordType.BRIGADIER);
        if (values == null) return success(word);

        boolean isPartial = false;
        for (var value : values) {
            if (value.equals(word)) return success(word);
            isPartial |= value.startsWith(word);
        }

        return isPartial ? partial() : syntaxError(start);
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        if (values == null) return;

        for (var value : values) {
            if (value.startsWith(raw)) {
                suggestion.add(value);
            }
        }
    }
}
