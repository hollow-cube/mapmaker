package net.hollowcube.mapmaker.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class FlySpeedCommand extends CommandDsl {
    private static final float DEFAULT_SPEED = 0.05f;

    private final Argument<Float> speedArg = Argument.Float("speed").clamp(0f, 10f)
            .description("How fast to fly (from 0 to 10)");
//            .errorHandler((sender, context) -> sender.sendMessage("invalid fly speed todo: "));

    public FlySpeedCommand() {
        super("flyspeed");

        description = "Changes how fast you fly when building";

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleSetFlySpeed), speedArg);
    }

    private void handleSetFlySpeed(@NotNull Player player, @NotNull CommandContext context) {
        float flySpeedArg = context.get(speedArg);

        player.setFlyingSpeed(DEFAULT_SPEED * flySpeedArg);
        player.sendMessage(MapMessages.COMMAND_FLY_SPEED_CHANGED.with(formatSpeed(flySpeedArg)));
    }

    private @NotNull String formatSpeed(float speed) {
        if (speed == (int) speed) {
            // If the float is an integer, format without decimal places
            return String.valueOf((int) speed);
        } else {
            // If the float has a fractional part, format with one decimal place
            return String.format("%.1f", speed);
        }
    }
}
