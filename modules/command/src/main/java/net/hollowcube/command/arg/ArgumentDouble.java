package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ArgumentDouble extends Argument<Double> {
    private double min = -Double.MAX_VALUE;
    private double max = Double.MAX_VALUE;

    ArgumentDouble(@NotNull String id) {
        super(id);
    }

    public @NotNull ArgumentDouble min(float min) {
        this.min = min;
        return this;
    }

    public @NotNull ArgumentDouble max(float max) {
        this.max = max;
        return this;
    }

    public @NotNull ArgumentDouble clamp(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public @NotNull ParseResult<Double> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
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
}
