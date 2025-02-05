package net.hollowcube.terraform.command.util;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.schem.Rotation;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ArgumentRotation extends Argument<Rotation> {

    private static final Map<String, Rotation> VALUES;
    static {
        Map<String, Rotation> values = new HashMap<>();
        List.of("0", "0.0").forEach(value -> values.put(value, Rotation.NONE));
        List.of("90", "90.0", "-270", "-270.0").forEach(value -> values.put(value, Rotation.CLOCKWISE_90));
        List.of("180", "180.0", "-180", "-180.0").forEach(value -> values.put(value, Rotation.CLOCKWISE_180));
        List.of("270", "270.0", "-90", "-90.0").forEach(value -> values.put(value, Rotation.CLOCKWISE_270));
        VALUES = Map.copyOf(values);
    }

    private final Set<Rotation> excluded;

    protected ArgumentRotation(@NotNull String id, @NotNull Rotation... excluded) {
        super(id);
        this.excluded = new HashSet<>(Arrays.asList(excluded));
    }

    public static ArgumentRotation of(@NotNull String id, @NotNull Rotation... excluded) {
        return new ArgumentRotation(id, excluded);
    }

    @Override
    public @NotNull ParseResult<Rotation> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        String line = reader.readWord(WordType.BRIGADIER);
        boolean isPartial = false;
        for (var entry : VALUES.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            Rotation value = entry.getValue();
            if (excluded.contains(value)) continue;
            if (key.equalsIgnoreCase(line)) return success(value);
            if (key.startsWith(line)) isPartial = true;
        }
        return isPartial ? partial() : syntaxError();
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        raw = raw.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (var entry : VALUES.entrySet()) {
            String key = entry.getKey();
            Rotation value = entry.getValue();
            if (excluded.contains(value)) continue;

            if (key.startsWith(raw)) {
                suggestions.add(key);
            }
        }
        suggestions.sort(Comparator.comparingInt(String::length).thenComparing(a -> a));
        suggestion.addAll(suggestions);
    }
}
