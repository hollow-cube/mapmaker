package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;

public class ArgumentInt extends Argument<Integer> {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    ArgumentInt(String id) {
        super(id);
    }

    /**
     * inclusive
     */
    public ArgumentInt min(int min) {
        this.min = min;
        return this;
    }

    /**
     * inclusive
     */
    public ArgumentInt max(int max) {
        this.max = max;
        return this;
    }

    /**
     * both inclusive
     */
    public ArgumentInt clamp(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public ParseResult<Integer> parse(CommandSender sender, StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER);
        try {
            var value = Integer.parseInt(word.trim());
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

    @Override
    public boolean shouldSuggest() {
        return false;
    }
}
