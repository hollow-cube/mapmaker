package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ArgumentEnum<E extends Enum<?>> extends Argument<E> {
    private final Class<E> enumClass;
    private final E[] values;

    ArgumentEnum(@NotNull String id, @NotNull Class<E> enumClass) {
        super(id);
        this.enumClass = enumClass;
        this.values = enumClass.getEnumConstants();
    }

    @Override
    public @NotNull ParseResult<E> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER).toLowerCase(Locale.ROOT);

        boolean isPartial = false;
        for (var value : values) {
            var name = value.name().toLowerCase(Locale.ROOT);
            if (name.equals(word)) return success(value);
            if (name.startsWith(word)) isPartial = true;
        }

        return isPartial ? partial() : syntaxError();
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        raw = raw.toLowerCase(Locale.ROOT);
        for (var value : values) {
            var name = value.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(raw)) suggestion.add(name);
        }
    }
}
