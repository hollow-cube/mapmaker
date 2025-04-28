package net.hollowcube.terraform.compat.worldedit.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ArgumentCommaSeparatedVec3 extends Argument<Vec> {
    ArgumentCommaSeparatedVec3(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Vec> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);
        var parts = word.split(",");

        try {
            return switch (parts.length) {
                case 1 -> success(new Vec(Double.parseDouble(parts[0])));
                case 3 -> success(new Vec(
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2])
                ));
                default -> syntaxError("Expected 1 or 3 values, got " + parts.length);
            };
        } catch (NumberFormatException ignored) {
            return syntaxError();
        }
    }

    @Override
    public boolean shouldSuggest() {
        return false;
    }
}
