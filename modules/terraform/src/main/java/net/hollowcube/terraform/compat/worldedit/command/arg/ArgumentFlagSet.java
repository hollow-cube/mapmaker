package net.hollowcube.terraform.compat.worldedit.command.arg;

import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Locale;

public class ArgumentFlagSet<E extends Enum<E>> extends Argument<EnumSet<E>> {
    private final Class<E> enumClass;
    private final E[] enumConstants;
    private final Char2IntMap flagMap;

    ArgumentFlagSet(@NotNull String id, @NotNull Class<E> enumClass) {
        super(id);
        this.enumClass = enumClass;
        this.enumConstants = enumClass.getEnumConstants();
        this.flagMap = new Char2IntArrayMap();
        for (var value : enumConstants) {
            var name = value.name().toLowerCase(Locale.ROOT);
            flagMap.put(name.charAt(0), value.ordinal());
        }
    }

    @Override
    public @NotNull ParseResult<EnumSet<E>> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER).toLowerCase(Locale.ROOT);
        if (word.isEmpty()) return partial();
        if (word.charAt(0) != '-') return syntaxError();

        var flags = EnumSet.noneOf(enumClass);
        for (int i = 1; i < word.length(); i++) {
            char c = word.charAt(i);
            if (!flagMap.containsKey(c)) return syntaxError("Unknown flag: " + c);
            flags.add(enumClass.getEnumConstants()[flagMap.get(c)]);
        }

        return success(flags);
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        if (raw.isEmpty()) {
            for (var value : flagMap.keySet()) {
                suggestion.add("-" + value);
            }
        } else if (raw.charAt(0) == '-') {
            suggestion.setStart(suggestion.getStart() + 1);
            var word = raw.substring(1).toLowerCase(Locale.ROOT);
            for (var value : flagMap.keySet()) {
                if (!word.contains(String.valueOf(value))) {
                    suggestion.setStart(suggestion.getStart() + word.length());
                    suggestion.add(String.valueOf(value));
                }
            }
        }
    }
}
