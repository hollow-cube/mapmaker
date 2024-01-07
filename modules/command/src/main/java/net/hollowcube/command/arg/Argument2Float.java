package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Argument2Float extends Argument2<Float> {
    private float min = -Float.MAX_VALUE;
    private float max = Float.MAX_VALUE;

    Argument2Float(@NotNull String id) {
        super(id);
    }

    public @NotNull Argument2Float min(float min) {
        this.min = min;
        return this;
    }

    public @NotNull Argument2Float max(float max) {
        this.max = max;
        return this;
    }

    public @NotNull Argument2Float clamp(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public @NotNull ParseResult2<Float> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
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
