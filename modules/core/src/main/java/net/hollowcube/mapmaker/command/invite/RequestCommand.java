package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class RequestCommand extends Command {
    private final Argument<EntityFinder> targetArg = Argument.Opt(Argument.Entity("player")
            .singleEntity(true).onlyPlayers(true));

    private final PlayerInviteService inviteService;

    public RequestCommand(@NotNull PlayerInviteService inviteService) {
        super("request");
        this.inviteService = inviteService;

        category = CommandCategory.SOCIAL;

        addSyntax(playerOnly(this::handleRequest), targetArg);
    }

    private void handleRequest(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(context.getRaw(targetArg))));
            return;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        inviteService.registerRequest(player, target);
    }
}
