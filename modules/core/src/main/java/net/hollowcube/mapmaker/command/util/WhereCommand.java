package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.world.KindaBadThingToFix;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class WhereCommand extends Command {
    private final Argument<EntityFinder> targetArg = Argument.Opt(Argument.Entity("player")
            .singleEntity(true).onlyPlayers(true));

    public WhereCommand() {
        super("where", "whereis", "find");

        addSyntax(playerOnly(this::handleFindPlayer), targetArg);
    }

    private void handleFindPlayer(@NotNull Player player, @NotNull CommandContext context) {
        // If the target was not specified, use the player themselves.
        var targetFinder = context.get(targetArg);
        var target = targetFinder == null ? player : targetFinder.findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player_offline", Component.text(context.getRaw(targetArg))));
            return;
        }

        // If checking self, just say where you are.
        var senderMap = KindaBadThingToFix.getMapFromCurrentWorld(player);
        if (player.equals(target)) {
            if (senderMap == null) {
                player.sendMessage(Component.translatable("command.where.self.hub"));
            } else if (senderMap.isPublished()) {
                player.sendMessage(Component.translatable("command.where.self.playing" + senderMap.name()));
            } else if (!senderMap.isPublished()) {
                player.sendMessage(Component.translatable("command.where.self.building" + senderMap.name()));
                return;
            }

            // Otherwise find the other players info
            var targetMap = KindaBadThingToFix.getMapFromCurrentWorld(target);
            if (targetMap == null) {
                player.sendMessage(Component.translatable("command.where.hub", Component.translatable(target.getUsername())));
            } else if (targetMap == senderMap) {
                player.sendMessage(Component.translatable("command.where.same_map", Component.translatable(target.getUsername())));
            } else if (targetMap.isPublished()) {
                player.sendMessage(Component.translatable("command.where.playing", Component.translatable(target.getUsername()), Component.translatable(targetMap.name())));
            } else if (!targetMap.isPublished()) {
                player.sendMessage(Component.translatable("command.where.building", Component.translatable(target.getUsername())));
            } else throw new IllegalStateException("unreachable");
            player.sendMessage(Component.translatable("generic.unknown_error"));
        }
    }
}
