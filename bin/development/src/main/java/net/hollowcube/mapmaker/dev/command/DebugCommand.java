package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.dev.DevRuntime;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DebugCommand extends Command {
    private final PlayerStorage playerStorage;

    public DebugCommand(@NotNull PlayerStorage playerStorage) {
        super("debug");
        this.playerStorage = playerStorage;

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

        var playerData = PlayerData.fromPlayer(player);
        player.sendMessage(Component.text(playerData.getId() + " (" + playerData.getUsername() + ")"));
        player.sendMessage(Component.text("Display: ").append(playerData.getDisplayName()));
    }

    private void handlePlayerReset(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var playerData = PlayerData.fromPlayer(player);
        playerData.setUnlockedMapSlots(5);
        playerData.setMapSlots(new String[]{null, null, null, null, null});
        playerStorage.updatePlayer(playerData);
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
