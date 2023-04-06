package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.map.util.ScoreboardUtil;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.ui.Scoreboards;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.ScoreboardUtil.NoScoreboards;

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
            String parseArg = context.get("on/off");
            switch (parseArg) {
                case "on" -> {
                    var map = MapWorld.fromInstance(player.getInstance()).map();
                    sender.sendMessage("You have toggled scoreboards on!");
                    if (map.isPublished()) {
                        Scoreboards.showPlayerPlayingScoreboard(player, map);
                    } else if (!map.isPublished()) {
                        Scoreboards.showPlayerEditingScoreboard(player, map);
                    } else { // Default to lobby scoreboard
                        Scoreboards.showPlayerLobbyScoreboard(player);
                    }
                }
                case "off" -> {
                    Scoreboards.hidePlayerScoreboard(player);
                    sender.sendMessage("You have toggled scoreboards off!");
                }
            }
        }
    }
}
