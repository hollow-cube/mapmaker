package net.hollowcube.mapmaker.dev.command;

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

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("Debug command :O");
        });

        addSyntax(this::handlePlayerReset, ArgumentType.Literal("reset-self"));
    }

    private void handlePlayerReset(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("You must be a player to use this command!");
            return;
        }

        var playerData = PlayerData.fromPlayer(player);
        playerData.setUnlockedMapSlots(5);
        playerData.setMapSlots(new String[]{null, null, null, null, null});
        playerStorage.updatePlayer(playerData);
        player.sendMessage(Component.text("Bye bye data :)"));
    }
}
