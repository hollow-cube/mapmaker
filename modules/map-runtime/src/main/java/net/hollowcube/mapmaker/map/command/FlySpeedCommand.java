package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.mapmaker.runtime.building.command.BuildingConditions;
import net.minestom.server.entity.Player;

import static net.hollowcube.command.CommandCondition.or;
import static net.hollowcube.mapmaker.map.command.FlyCommand.playingOrSpectatingFilter;

public class FlySpeedCommand extends CommandDsl {
    private static final float DEFAULT_SPEED = 0.05f;

    private final Argument<Float> speedArg = Argument.Float("flyspeed").clamp(0f, 10f)
        .description("How fast to fly (from 0 to 10)");

    public FlySpeedCommand() {
        super("flyspeed");

        description = "Changes how fast you fly when building";

        setCondition(or(
            playingOrSpectatingFilter(),
            BuildingConditions.buildingWorld()
        ));

        addSyntax(playerOnly(this::handleSetFlySpeed), speedArg);
    }

    private void handleSetFlySpeed(Player player, CommandContext context) {
        float flySpeedArg = context.get(speedArg);

        player.setFlyingSpeed(DEFAULT_SPEED * flySpeedArg);
        player.sendMessage(
            TranslatableBuilder.of("command.flyspeed.changed")
                .with(formatSpeed(flySpeedArg))
                .build()
        );
    }

    private String formatSpeed(float speed) {
        if (speed == (int) speed) {
            // If the float is an integer, format without decimal places
            return String.valueOf((int) speed);
        } else {
            // If the float has a fractional part, format with one decimal place
            return String.format("%.1f", speed);
        }
    }
}
