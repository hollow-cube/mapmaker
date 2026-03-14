package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArgumentWord extends Argument<String> {
    private @Nullable List<String> values = null;

    ArgumentWord(String id) {
        super(id);
    }

    public ArgumentWord with(String... values) {
        this.values = List.of(values);
        return this;
    }

    public ArgumentWord with(List<String> values) {
        this.values = List.copyOf(values);
        return this;
    }

    @Override
    public ParseResult<String> parse(CommandSender sender, StringReader reader) {
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
    public void suggest(CommandSender sender, String raw, Suggestion suggestion) {
        if (values == null) return;

        for (var value : values) {
            if (value.startsWith(raw)) {
                suggestion.add(value);
            }
        }
    }

    @Override
    public boolean shouldSuggest() {
        return this.values != null && !this.values.isEmpty();
    }
}
