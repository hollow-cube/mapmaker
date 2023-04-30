package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.world.MapWorldNew;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
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
    private final MapStorage mapStorage;

    public DebugCommand(@NotNull PlayerStorage playerStorage, @NotNull MapStorage mapStorage) {
        super("debug");
        this.playerStorage = playerStorage;
        this.mapStorage = mapStorage;

        setDefaultExecutor((sender, context) -> sender.sendMessage("Debug command :O"));

        addSyntax(this::handlePlayerReset, ArgumentType.Literal("reset-self"));
        addSyntax(this::handleMapWorldDebug, ArgumentType.Literal("world"));
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

        var world = MapWorldNew.forPlayerOptional(player);
        if (world == null) {
            player.sendMessage("You are not in a map world!");
            return;
        }

        player.sendMessage(Component.text("Map: ").append(world.map().getNameComponent()));
        player.sendMessage("Type: " + world.getClass().getSimpleName());
    }
}
