package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.mapmaker.ui.Scoreboards;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleScoreboardCommand extends Command {
    public ToggleScoreboardCommand() {
        super("scoreboards", "togglescoreboards", "sb", "tsb");
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /scoreboards <on/off>"));
        addSyntax(this::parseArgument, ArgumentType.String("on/off"));
    }

    private void parseArgument(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
        } else {
//            String parseArg = context.get("on/off");
//            switch (parseArg) {
//                case "on" -> {
//                    Scoreboards.setScoreboardVisibility(player, Boolean.TRUE);
//                    sender.sendMessage("You have toggled scoreboards on!");
//                }
//                case "off" -> {
//                    Scoreboards.setScoreboardVisibility(player, Boolean.FALSE);
//                    sender.sendMessage("You have toggled scoreboards off!");
//                }
//            }
        }
    }
}
