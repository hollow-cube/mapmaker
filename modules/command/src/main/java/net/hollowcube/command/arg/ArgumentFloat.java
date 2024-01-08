package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ArgumentFloat extends Argument<Float> {
    private float min = -Float.MAX_VALUE;
    private float max = Float.MAX_VALUE;

    ArgumentFloat(@NotNull String id) {
        super(id);
    }

    public @NotNull ArgumentFloat min(float min) {
        this.min = min;
        return this;
    }

    public @NotNull ArgumentFloat max(float max) {
        this.max = max;
        return this;
    }

    public @NotNull ArgumentFloat clamp(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public @NotNull ParseResult<Float> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER);
        try {
            var value = Float.parseFloat(word);
            if (Float.isNaN(value) || Float.isInfinite(value)) return syntaxError();
            if (value < min || value > max) return syntaxError(); //todo custom message
            return success(value);
        } catch (NumberFormatException e) {
            return syntaxError();
        }
    }
}
