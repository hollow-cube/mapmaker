package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;

public class ArgumentFloat extends Argument<Float> {
    private float min = -Float.MAX_VALUE;
    private float max = Float.MAX_VALUE;

    ArgumentFloat(String id) {
        super(id);
    }

    public ArgumentFloat min(float min) {
        this.min = min;
        return this;
    }

    public ArgumentFloat max(float max) {
        this.max = max;
        return this;
    }

    public ArgumentFloat clamp(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public ParseResult<Float> parse(CommandSender sender, StringReader reader) {
        var word = reader.readWord(WordType.BRIGADIER);
        try {
            var value = Float.parseFloat(word);
            if (Float.isNaN(value) || Float.isInfinite(value)) return syntaxError();
            if (value < min) return syntaxError("%s must be at least %.2f".formatted(this.id(), this.min));
            if (value > max) return syntaxError("%s must be at most %.2f".formatted(this.id(), this.max));
            return success(value);
        } catch (NumberFormatException e) {
            return syntaxError();
        }
    }

   @Override
   public void properties(NetworkBuffer buffer) {
       boolean hasMax = this.max != Float.MAX_VALUE, hasMin = this.min != -Float.MAX_VALUE;
       buffer.write(NetworkBuffer.BYTE, ArgumentUtils.createNumberFlags(hasMin, hasMax));
       if (hasMin) {
           buffer.write(NetworkBuffer.FLOAT, this.min);
       }
       if (hasMax) {
           buffer.write(NetworkBuffer.FLOAT, this.max);
       }
   }

   @Override
   public ArgumentParserType argumentType() {
       return ArgumentParserType.FLOAT;
   }

    @Override
    public boolean shouldSuggest() {
        return false;
    }
}
