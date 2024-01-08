package net.hollowcube.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class TeleportCommand extends CommandDsl {
    private final Argument<EntityFinder> targetArg = Argument.Entity("target").singleEntity(true).onlyPlayers(true);

    public TeleportCommand() {
        super("tp");
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleTeleportToTarget), targetArg);
    }

    private void handleTeleportToTarget(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player_offline", Component.translatable(context.getRaw(targetArg))));
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
        player.teleport(target.getPosition()).thenRun(() -> {
            player.sendMessage(Component.translatable("teleport.success", Component.translatable(target.getUsername())));
        });
    }
}
