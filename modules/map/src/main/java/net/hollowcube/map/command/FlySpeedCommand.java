package net.hollowcube.map.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlySpeedCommand extends BaseMapCommand {

        public FlySpeedCommand() {
            super(true, "flyspeed", "fs");
            addSyntax(this::setFlySpeed, ArgumentType.Float("flyspeed").between(0f, 10f));
        }
        //TODO make something better than these float values
        private void setFlySpeed(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command!");
            } else {
                float flySpeedArg = context.getOrDefault("flyspeed", 1.0f);
                if (flySpeedArg <=.5f) {
                    player.setFlyingSpeed(0.0f);
                    player.sendMessage("Flying speed set to 0.");
                } else {
                    player.setFlyingSpeed((flySpeedArg / 10.0f) - .05f);
                    player.sendMessage("Flying speed set to " + flySpeedArg + ".");
            }
        }
    }
}
