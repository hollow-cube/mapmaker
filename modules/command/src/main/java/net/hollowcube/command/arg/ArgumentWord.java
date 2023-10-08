package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ArgumentWord extends Argument<String> {
    private List<String> values = null;

    public ArgumentWord(@NotNull String id) {
        super(id);
    }

    public @NotNull ArgumentWord with(@NotNull String... values) {
        this.values = List.of(values);
        return this;
    }

    @Override
    public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER);
        if (values == null) {
            if (word.isEmpty()) return new ParsePartial<>();
            return new ParseSuccess<>(word);
        }

        boolean isPartial = false;
        for (var value : values) {
            if (value.equals(word)) return new ParseSuccess<>(word);
            isPartial |= value.startsWith(word);
        }

        return isPartial ? new ParsePartial<>() : new ParseFailure<>();
    }

    @Override
    public @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var pos = reader.pos();
        var word = reader.readWord(WordType.ALPHANUMERIC);

        var suggestions = new ArrayList<SuggestionEntry>();
        for (var value : values) {
            if (value.startsWith(word)) {
                suggestions.add(new SuggestionEntry(value, null));
            }
        }

        return new SuggestionResult.Success(pos, word.length(), suggestions);
    }

    @Override
    public String toString() {
        return "word@" + id();
    }
}
