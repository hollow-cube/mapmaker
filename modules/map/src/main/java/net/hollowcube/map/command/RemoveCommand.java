package net.hollowcube.map.command;

import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RemoveCommand extends BaseMapCommand {
    private final MapToHubBridge bridge;

    public RemoveCommand(@NotNull MapToHubBridge bridge) {
        super(true, "remove");
        this.bridge = bridge;

        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.translatable("command.remove.usage")));
        addSyntax(this::remove, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void remove(@NotNull CommandSender sender, @NotNull CommandContext context) {
        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        if (target == null) {
            sender.sendMessage(Component.translatable("generic.player_offline"));
            return;
        }

        var senderMap = MapWorld.forPlayerOptional((Player) sender);
        var targetMap = MapWorld.forPlayerOptional((Player) target);
        if (!(targetMap == senderMap)) {
            sender.sendMessage(Component.translatable("map.build.remove.same_map"));
            return;
        }

        if (!(senderMap == null) && doesPlayerOwnMap((Player) sender, senderMap)) {

            try {
                var world = MapWorld.forPlayerOptional(target);
                if (world instanceof InternalMapWorld internalWorld) {
                    internalWorld.removePlayer(target);
                }

                bridge.sendPlayerToHub(target);
                target.sendMessage(Component.translatable("map.build.removed", Component.text(((Player) sender).getUsername())));
                sender.sendMessage(Component.translatable("map.build.remove", Component.text((target).getUsername())));

            } catch (Exception e) {
                System.out.println("something went wrong when removing");
            }
        } else {
            sender.sendMessage(Component.translatable("map.build.cant_remove"));
        }
    }

    private static boolean doesPlayerOwnMap(@NotNull Player player, @NotNull MapWorld mapWorld) {
        return player.getUuid().equals(UUID.fromString(mapWorld.map().owner()));
    }
}