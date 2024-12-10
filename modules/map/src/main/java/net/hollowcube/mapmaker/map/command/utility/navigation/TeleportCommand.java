package net.hollowcube.mapmaker.map.command.utility.navigation;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class TeleportCommand extends CommandDsl {
    private final Argument<EntityFinder> targetArg = Argument.Entity("target").singleEntity(true).onlyPlayers(true).sameWorld(true)
            .description("The player to teleport to");

    public TeleportCommand() {
        super("tp");

        category = CommandCategories.MAP;
        description = "Teleports you to a player";

        setCondition(CommandCondition.or(
                // Always allowed in editing maps for anyone
                mapFilter(false, true, false),
                // Allowed in playing maps for people in spectator mode
                (sender, context) -> {
                    if (!(sender instanceof Player player)) return CommandCondition.HIDE;
                    if (!(MapWorld.forPlayerOptional(player) instanceof PlayingMapWorld world))
                        return CommandCondition.HIDE;
                    return world.isSpectating(player) ? CommandCondition.ALLOW : CommandCondition.HIDE;
                }
        ));

        addSyntax(playerOnly(this::handleTeleportToTarget), targetArg);
    }

    private void handleTeleportToTarget(@NotNull Player player, @NotNull CommandContext context) {
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
        player.teleport(target.getPosition(), Vec.ZERO, null, RelativeFlags.NONE).thenRun(() -> {
            player.sendMessage(Component.translatable("teleport.success", Component.translatable(target.getUsername())));
        });
    }
}
