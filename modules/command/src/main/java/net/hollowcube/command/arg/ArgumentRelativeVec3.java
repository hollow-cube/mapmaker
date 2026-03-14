package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.common.util.PlayerUtil;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class ArgumentRelativeVec3 extends Argument<Point> {

    ArgumentRelativeVec3(String id) {
        super(id);
    }

    @Override
    public ParseResult<Point> parse(CommandSender sender, StringReader reader) {
        var origin = (sender instanceof Player player) ? player.getPosition() : Vec.ZERO;
        var x = readCoordinate(origin.x(), reader);
        if (x == null) return partial();
        var y = readCoordinate(origin.y(), reader);
        if (y == null) return partialWithValue(new Vec(x, origin.y(), origin.z()));
        var z = readCoordinate(origin.z(), reader);
        if (z == null) return partialWithValue(new Vec(x, y, origin.z()));
        return new ParseResult.Success<>(new Vec(x, y, z));
    }

    private @Nullable Double readCoordinate(double origin, StringReader reader) {
        var word = reader.readWord(WordType.GREEDY);
        var isRelative = !word.isEmpty() && word.charAt(0) == '~';
        if (isRelative) word = word.substring(1);
        if (word.isEmpty()) return isRelative ? origin : null;

        try {
            return Double.parseDouble(word) + (isRelative ? origin : 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void suggest(CommandSender sender, String raw, Suggestion suggestion) {
        if (sender instanceof Player player) {
            var targetBlockPosition = PlayerUtil.getTargetBlock(player, PlayerUtil.DEFAULT_PLACEMENT_DISTANCE, false);
            var coordinateIndex = raw.split(" ").length;
            if (targetBlockPosition != null) {
                int x = targetBlockPosition.blockX(), y = targetBlockPosition.blockY(), z = targetBlockPosition.blockZ();
                suggest(suggestion, coordinateIndex, raw, x, y, z);
            } else {
                suggest(suggestion, coordinateIndex, raw, "~", "~", "~");
            }
        }
        super.suggest(sender, raw, suggestion);
    }

    private void suggest(Suggestion suggestion, @Range(from = 1, to = 4) int coordinateIndex, String raw, Object first, Object second, Object third) {
        if (coordinateIndex == 1) {
            suggestion.add("%s".formatted(first));
            suggestion.add("%s %s".formatted(first, second));
            suggestion.add("%s %s %s".formatted(first, second, third));
        } else if (coordinateIndex == 2) {
            suggestion.add("%s %s".formatted(raw, third));
        }
    }

    @Override
    public void properties(NetworkBuffer buffer) {}

    @Override
    public ArgumentParserType argumentType() {
        return ArgumentParserType.VEC3;
    }
}
