package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArgumentWord extends Argument<String> {
    private List<String> values = null;

    ArgumentWord(@NotNull String id) {
        super(id);
    }

    public @NotNull ArgumentWord with(@NotNull String... values) {
        this.values = List.of(values);
        return this;
    }

    public @NotNull ArgumentWord with(@NotNull List<String> values) {
        this.values = List.copyOf(values);
        return this;
    }

    @Override
    public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER);
        if (values == null) return new ParseSuccess<>(word);

        boolean isPartial = false;
        for (var value : values) {
            if (value.equals(word)) return new ParseSuccess<>(word);
            isPartial |= value.startsWith(word);
        }

        return isPartial ? new ParsePartial<>() : new ParseFailure<>();
    }

    @Override
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        if (values == null) return;

        var word = reader.readWord(WordType.ALPHANUMERIC);
        for (var value : values) {
            if (value.startsWith(word)) {
                suggestion.add(value);
            }
        }
    }

    @Override
    public String toString() {
        return "word@" + id();
    }
}
