package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;

public class ArgumentDouble extends Argument<Double> {
    private double min = -Double.MAX_VALUE;
    private double max = Double.MAX_VALUE;

    ArgumentDouble(String id) {
        super(id);
    }

    public ArgumentDouble min(float min) {
        this.min = min;
        return this;
    }

    public ArgumentDouble max(float max) {
        this.max = max;
        return this;
    }

    public ArgumentDouble clamp(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public ParseResult<Double> parse(CommandSender sender, StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER);
        try {
            var value = Double.parseDouble(word);
            if (Double.isNaN(value) || Double.isInfinite(value)) return syntaxError();
            if (value < min) return syntaxError("%s must be at least %.2f".formatted(this.id(), this.min));
            if (value > max) return syntaxError("%s must be at most %.2f".formatted(this.id(), this.max));
            return success(value);
        } catch (NumberFormatException e) {
            return syntaxError();
        }
    }

    @Override
    public void properties(NetworkBuffer buffer) {
        boolean hasMax = this.max != Double.MAX_VALUE, hasMin = this.min != -Double.MAX_VALUE;
        buffer.write(NetworkBuffer.BYTE, ArgumentUtils.createNumberFlags(hasMin, hasMax));
        if (hasMin) {
            buffer.write(NetworkBuffer.DOUBLE, this.min);
        }
        if (hasMax) {
            buffer.write(NetworkBuffer.DOUBLE, this.max);
        }
    }

    @Override
    public ArgumentParserType argumentType() {
        return ArgumentParserType.DOUBLE;
    }

    @Override
    public boolean shouldSuggest() {
        return false;
    }
}
