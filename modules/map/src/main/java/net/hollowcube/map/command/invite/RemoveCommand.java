package net.hollowcube.map.command.invite;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class RemoveCommand extends Command {

    private final Argument<EntityFinder> targetArg = Argument.Entity("player").onlyPlayers(true);

    private final MapToHubBridge bridge;

    public RemoveCommand(@NotNull MapToHubBridge bridge) {
        super("remove");
        this.bridge = bridge;
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleRemoveTarget), targetArg);
    }

    private void handleRemoveTarget(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player_offline"));
            return;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        var senderMap = MapWorld.forPlayer(player); // Always present due to command condition
        if (!doesPlayerOwnMap(player, senderMap)) {
            player.sendMessage(Component.translatable("map.build.cant_remove"));
            return;
        }
        var targetMap = MapWorld.forPlayerOptional(target);
        if (!senderMap.equals(targetMap)) {
            player.sendMessage(Component.translatable("map.build.remove.same_map"));
            return;
        }

        // All preconditions OK, actually remove the player from the map.
        try {
            var world = MapWorld.forPlayerOptional(target);
            if (world instanceof InternalMapWorld internalWorld) {
                internalWorld.removePlayer(target);
            }

            bridge.sendPlayerToHub(target);
            target.sendMessage(Component.translatable("map.build.removed", Component.text(player.getUsername())));
            player.sendMessage(Component.translatable("map.build.remove", Component.text(target.getUsername())));

        } catch (Exception e) {
            throw new RuntimeException("failed to remove player from map", e);
        }
    }

    private static boolean doesPlayerOwnMap(@NotNull Player player, @NotNull MapWorld mapWorld) {
        return player.getUuid().equals(UUID.fromString(mapWorld.map().owner()));
    }
}
