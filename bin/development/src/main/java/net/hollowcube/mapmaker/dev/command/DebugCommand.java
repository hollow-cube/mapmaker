package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.dev.runtime.DevRuntime;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DebugCommand extends Command {
    private final PlayerService playerService;

    public DebugCommand(@NotNull PlayerService playerService) {
        super("debug");
        this.playerService = playerService;

//        setDefaultExecutor((sender, context) -> sender.sendMessage("Debug command :O"));

//        addSyntax(playerOnly(this::handleReloadCommands), Argument.Literal("reload-commands"));

//        addSyntax(playerOnly(this::handleDebugResourcePack), Argument.Literal("rp"));
//        addSyntax(playerOnly(this::handleDebugSelf), Argument.Literal("self"));
//        addSyntax(playerOnly(this::handleMapWorldDebug), Argument.Literal("world"));

        addSyntax(playerOnly(this::handleBlockDebug), Argument.Literal("block"));

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

    private void handleBlockDebug(@NotNull Player player, @NotNull CommandContext context) {
        var blockPosition = player.getTargetBlockPosition(5);
        if (blockPosition == null) {
            player.sendMessage("No block in range!");
            return;
        }

        var block = player.getInstance().getBlock(blockPosition);
        player.sendMessage("Block: " + block);
    }

    private void handleReloadCommands(@NotNull Player player, @NotNull CommandContext context) {
        player.refreshCommands();
        player.sendMessage("Reloaded commands!");
    }
}
