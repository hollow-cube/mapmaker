package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArgumentRelativeVec3 extends Argument<Point> {
    ArgumentRelativeVec3(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Point> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var origin = (sender instanceof Player player) ? player.getPosition() : Vec.ZERO;
        var x = readCoordinate(origin.x(), reader);
        if (x == null) return new ParseFailure<>();
        var y = readCoordinate(origin.y(), reader);
        if (y == null) return new ParseFailure<>();
        var z = readCoordinate(origin.z(), reader);
        if (z == null) return new ParseFailure<>();
        return new ParseSuccess<>(new Vec(x, y, z));
    }

    private @Nullable Double readCoordinate(double origin, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.GREEDY);
        var isRelative = !word.isEmpty() && word.charAt(0) == '~';
        if (isRelative) word = word.substring(1);

        try {
            return Double.parseDouble(word) + (isRelative ? origin : 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
