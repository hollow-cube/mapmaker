package net.hollowcube.map.command.utility;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.map.lang.MapMessages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class FlySpeedCommand extends Command {
    private final Argument<Float> speedArg = Argument.Float("speed").clamp(0f, 10f).defaultValue(1.0f);

    public FlySpeedCommand() {
        super("flyspeed");
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleSetFlySpeed), speedArg);
    }

    private void handleSetFlySpeed(@NotNull Player player, @NotNull CommandContext context) {
        float flySpeedArg = context.get(speedArg);
        if (flySpeedArg <= .5f) {
            player.setFlyingSpeed(0.0f);
            player.sendMessage(MapMessages.COMMAND_FLY_SPEED_CHANGED.with(0));
        } else {
            player.setFlyingSpeed((flySpeedArg / 10.0f) - .05f);
            player.sendMessage(MapMessages.COMMAND_FLY_SPEED_CHANGED.with(flySpeedArg));
        }
    }
}
