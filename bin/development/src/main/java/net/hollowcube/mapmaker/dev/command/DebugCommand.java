package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.dev.runtime.DevRuntime;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DebugCommand extends Command {
    private final PlayerService playerService;

    public DebugCommand(@NotNull PlayerService playerService, @NotNull PermManager permManager, @NotNull MapService mapService) {
        super("debug");
        this.playerService = playerService;

        // Sys command is restricted
        addSubcommand(new SysCommand(permManager, mapService));

        // Mapmaker stuff
        subcommand("rp", playerOnly(this::handleDebugResourcePack), null);
        subcommand("self", playerOnly(this::handleDebugSelf), null);
        subcommand("world", playerOnly(this::handleMapWorldDebug), null);

        // Minestom stuff
        subcommand("commands", playerOnly(this::handleCommandsDebug), null);
        subcommand("block", playerOnly(this::handleBlockDebug), null);

//        addSyntax((sender, context) -> sender.sendMessage("Debug command :O"));
    }

    private void handleDebugResourcePack(@NotNull Player player, @NotNull CommandContext context) {
        var runtime = (DevRuntime) ServerRuntime.getRuntime();
        player.sendMessage(Component.text("Resource pack: " + runtime.resourcePackSha1()));
    }

    private void handleDebugSelf(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = PlayerDataV2.fromPlayer(player);
        player.sendMessage(Component.text(playerData.id() + " (" + playerData.username() + ")"));
        player.sendMessage(Component.text("Display: ").append(Component.text(playerData.username())));
        player.sendMessage(Component.text("Settings: "));
        player.sendMessage(Component.text("  scoreboards: " + playerData.settings().isScoreboardEnabled()));

        var mapPlayerData = MapPlayerData.fromPlayer(player);
        player.sendMessage(Component.text("Last played: " + mapPlayerData.lastPlayedMap()));
        player.sendMessage(Component.text("Last edited: " + mapPlayerData.lastEditedMap()));

    }

    private void handleMapWorldDebug(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) {
            player.sendMessage("You are not in a map world!");
            return;
        }

        player.sendMessage(Component.text("Map: ").append(Component.text(world.map().id())));
        player.sendMessage("Type: " + world.getClass().getSimpleName());
    }

    private void handleCommandsDebug(@NotNull Player player, @NotNull CommandContext context) {
        player.refreshCommands();
        player.sendMessage("Commands refreshed!");
    }

    private void handleBlockDebug(@NotNull Player player, @NotNull CommandContext context) {
        var blockPosition = player.getTargetBlockPosition(5);
        if (blockPosition == null) {
            player.sendMessage("No block in range!");
            return;
        }

        var block = player.getInstance().getBlock(blockPosition);
        player.sendMessage("Block: " + block);
    }

    private @NotNull Command subcommand(@NotNull String name, @NotNull CommandExecutor handler, @Nullable CommandCondition condition) {
        var cmd = new Command(name) {
        };
        cmd.setCondition(condition);
        cmd.addSyntax(handler);
        addSubcommand(cmd);
        return cmd;
    }
}
