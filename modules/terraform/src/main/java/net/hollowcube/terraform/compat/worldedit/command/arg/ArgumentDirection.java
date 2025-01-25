package net.hollowcube.terraform.compat.worldedit.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class ArgumentDirection extends Argument<@Nullable Direction> {
    private enum DirectionName {
        FORWARD, BACK, LEFT, RIGHT,
        NORTH, SOUTH, EAST, WEST,
        UP, DOWN,
        ME,
    }

    ArgumentDirection(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<@Nullable Direction> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        if (!(sender instanceof Player player)) return syntaxError();

        var word = reader.readWord(WordType.ALPHANUMERIC).toUpperCase(Locale.ROOT);
        if (word.isEmpty()) return partial();

        // Find the matching direction name from the input string
        boolean partial = false;
        DirectionName found = null;
        for (DirectionName value : DirectionName.values()) {
            if (value.name().equals(word)) {
                found = value;
                break;
            }
            if (value.name().startsWith(word)) {
                partial = true;
            }
        }
        if (found == null) {
            return partial ? partial() : syntaxError();
        }

        // We have a full match, convert it to a Direction
        return success(switch (found) {
            case NORTH, SOUTH, EAST, WEST, UP, DOWN -> Direction.valueOf(found.name());
            case ME -> DirectionUtil.fromView(player.getPosition());
            case FORWARD -> DirectionUtil.fromYaw(player.getPosition());
            case BACK -> DirectionUtil.fromYaw(player.getPosition()).opposite();
            case LEFT -> DirectionUtil.rotate(DirectionUtil.fromYaw(player.getPosition()), false);
            case RIGHT -> DirectionUtil.rotate(DirectionUtil.fromYaw(player.getPosition()), true);
        });
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        raw = raw.toLowerCase(Locale.ROOT);
        for (DirectionName value : DirectionName.values()) {
            var name = value.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(raw)) suggestion.add(name);
        }
    }

    static @NotNull Direction getDefault(@NotNull CommandSender sender) {
        if (sender instanceof Player player)
            return DirectionUtil.fromView(player.getPosition());
        return Direction.NORTH;
    }
}
