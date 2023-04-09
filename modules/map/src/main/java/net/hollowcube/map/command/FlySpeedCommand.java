package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.lang.MapMessages;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlySpeedCommand extends BaseMapCommand {
    private final Argument<Float> speedArg = ArgumentType.Float("flyspeed").between(0f, 10f);

    public FlySpeedCommand() {
        super(true, "flyspeed", "fs");

        setDefaultExecutor(this::setFlySpeed);
        addSyntax(this::setFlySpeed, speedArg);
    }

    //TODO make something better than these float values
    private void setFlySpeed(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        float flySpeedArg = context.getOrDefault(speedArg, 1.0f);
        if (flySpeedArg <= .5f) {
            player.setFlyingSpeed(0.0f);
            player.sendMessage(MapMessages.COMMAND_FLY_SPEED_CHANGED.with(0));
        } else {
            player.setFlyingSpeed((flySpeedArg / 10.0f) - .05f);
            player.sendMessage(MapMessages.COMMAND_FLY_SPEED_CHANGED.with(flySpeedArg));
        }

    }
}
