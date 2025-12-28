package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;

import static net.hollowcube.mapmaker.runtime.parkour.command.ParkourConditions.spectatorOnly;

public class TeleportCommand extends CommandDsl {
    private final Argument<EntityFinder> targetArg = Argument.Entity("target").singleEntity(true).onlyPlayers(true).sameWorld(true)
        .description("The player to teleport to");
    private final Argument<Point> locArg = Argument.RelativeVec3("location")
        .description("The location to teleport to");

    public TeleportCommand() {
        super("tp");

        category = CommandCategories.MAP;
        description = "Teleports you to a location or player";

        // Allowed in playing maps for people in spectator mode
        setCondition(spectatorOnly(true));

        addSyntax(playerOnly(this::handleTeleportToLocation), locArg);
        addSyntax(playerOnly(this::handleTeleportToTarget), targetArg);
    }

    private void handleTeleportToTarget(Player player, CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.translatable(context.getRaw(targetArg))));
            return;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.translatable("teleport.self"));
            return;
        }

        // Ensure the same instance
        var playerInstance = player.getInstance();
        var targetInstance = target.getInstance();
        if (!playerInstance.equals(targetInstance)) {
            player.sendMessage(Component.translatable("teleport.not_same_map"));
            return;
        }

        // Actually do the teleport
        MapWorldHelpers.teleportPlayer(player, target.getPosition()).thenRun(() ->
            player.sendMessage(Component.translatable("teleport.target.success", Component.translatable(target.getUsername())))
        );
    }

    private void handleTeleportToLocation(Player player, CommandContext context) {
        var loc = context.get(locArg);
        var instance = player.getInstance();
        if (instance.getWorldBorder().inBounds(loc)) {
            MapWorldHelpers.teleportPlayer(player, loc).thenRun(() ->
                player.sendMessage(Component.translatable("teleport.location.success", CoordinateUtil.asTranslationArgs(loc)))
            );
        } else {
            player.sendMessage(Component.translatable("teleport.out_of_bounds"));
        }
    }
}
