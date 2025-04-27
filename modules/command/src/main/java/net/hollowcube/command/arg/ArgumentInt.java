package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public class ArgumentInt extends Argument<Integer> {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    ArgumentInt(@NotNull String id) {
        super(id);
    }

    /**
     * inclusive
     *
     * @param min
     * @return
     */
    public @NotNull ArgumentInt min(int min) {
        this.min = min;
        return this;
    }

    /**
     * inclusive
     *
     * @param max
     * @return
     */
    public @NotNull ArgumentInt max(int max) {
        this.max = max;
        return this;
    }

    /**
     * both inclusive
     */
    public @NotNull ArgumentInt clamp(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public @NotNull ParseResult<Integer> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.ALPHANUMERIC);
        try {
            var value = Integer.parseInt(word);
            if (value < min) return syntaxError("%s must be at least %d".formatted(this.id(), this.min));
            if (value > max) return syntaxError("%s must be at most %d".formatted(this.id(), this.max));
            return success(value);
        } catch (NumberFormatException e) {
            return syntaxError();
        }
    }

    @Override
    public void properties(NetworkBuffer buffer) {
        boolean hasMax = this.max != Integer.MAX_VALUE, hasMin = this.min != Integer.MIN_VALUE;
        buffer.write(NetworkBuffer.BYTE, ArgumentUtils.createNumberFlags(hasMin, hasMax));
        if (hasMin) {
            buffer.write(NetworkBuffer.INT, this.min);
        }
        if (hasMax) {
            buffer.write(NetworkBuffer.INT, this.max);
        }
    }

    @Override
    public ArgumentParserType argumentType() {
        return ArgumentParserType.INTEGER;
    }
}
