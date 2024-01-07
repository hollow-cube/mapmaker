package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Argument2Int extends Argument2<Integer> {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    Argument2Int(@NotNull String id) {
        super(id);
    }

    /**
     * inclusive
     *
     * @param min
     * @return
     */
    public @NotNull Argument2Int min(int min) {
        this.min = min;
        return this;
    }

    /**
     * inclusive
     *
     * @param max
     * @return
     */
    public @NotNull Argument2Int max(int max) {
        this.max = max;
        return this;
    }

    /**
     * both inclusive
     */
    public @NotNull Argument2Int clamp(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public @NotNull ParseResult2<Integer> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.ALPHANUMERIC);
        try {
            var value = Integer.parseInt(word);
            if (value < min || value > max) return syntaxError();
            return success(value);
        } catch (NumberFormatException e) {
            return syntaxError();
        }
    }
}
