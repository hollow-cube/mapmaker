package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.dev.DevRuntime;
import net.hollowcube.mapmaker.player.PlayerDataUpdateRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DebugCommand extends Command {
    private final PlayerService playerService;

    public DebugCommand(@NotNull PlayerService playerService) {
        super("debug");
        this.playerService = playerService;

        setDefaultExecutor((sender, context) -> sender.sendMessage("Debug command :O"));

        addSyntax(this::handleDebugResourcePack, ArgumentType.Literal("rp"));
        addSyntax(this::handleDebugSelf, ArgumentType.Literal("self"));
        addSyntax(this::handlePlayerReset, ArgumentType.Literal("reset-self"));
        addSyntax(this::handleMapWorldDebug, ArgumentType.Literal("world"));
    }

    private void handleDebugResourcePack(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var runtime = (DevRuntime) ServerRuntime.getRuntime();
        player.sendMessage(Component.text("Resource pack: " + runtime.resourcePackSha1()));
    }

    private void handleDebugSelf(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        player.sendMessage(Component.text(playerData.id() + " (" + playerData.username() + ")"));
        player.sendMessage(Component.text("Display: ").append(Component.text(playerData.username())));
        player.sendMessage(Component.text("Last played: " + playerData.getLastPlayedMap()));
        player.sendMessage(Component.text("Last edited: " + playerData.getLastEditedMap()));
        player.sendMessage(Component.text("Settings: "));
        player.sendMessage(Component.text("  scoreboards: " + playerData.settings().isScoreboardEnabled()));
    }

    private void handlePlayerReset(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        playerData.setUnlockedMapSlots(5);
        for (int i = 0; i < PlayerDataV2.MAX_MAP_SLOTS; i++) {
            playerData.setMapSlot(i, null);
        }

        var req = new PlayerDataUpdateRequest().setUnlockedMapSlots(playerData.getUnlockedMapSlots()).setMapSlots(playerData.getRawMapSlots());
        playerService.updatePlayerData(playerData.id(), req);

        player.sendMessage(Component.text("Bye bye data :)"));
    }

    private void handleMapWorldDebug(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) {
            player.sendMessage("You are not in a map world!");
            return;
        }

        player.sendMessage(Component.text("Map: ").append(Component.text(world.map().id())));
        player.sendMessage("Type: " + world.getClass().getSimpleName());
    }
}
