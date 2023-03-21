package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.map.util.ScoreboardUtil;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.hub.world.HubWorld;
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
                    player.setTag(NoScoreboards, false);
                    sender.sendMessage("You have toggled scoreboards on!");
                    if (!(HubWorld.fromInstance(player.getInstance()) == null)) { //need to check if the player is in the lobby first
                        ScoreboardUtil.sendLobbyScoreboard(player);
                    } else if (MapWorld.fromInstance(player.getInstance()).map().isPublished()) {
                        ScoreboardUtil.sendPlayingScoreboard(player);
                    } else if (!MapWorld.fromInstance(player.getInstance()).map().isPublished()) {
                        ScoreboardUtil.sendEditingScoreboard(player);
                    } else { //just in case something goes wrong, default to the lobby scoreboard
                        ScoreboardUtil.sendLobbyScoreboard(player);
                    }
                }
                case "off" -> {
                    player.setTag(NoScoreboards, true);
                    ScoreboardUtil.removeScoreboard(player);
                    sender.sendMessage("You have toggled scoreboards off!");
                }
            }
        }
    }
}
