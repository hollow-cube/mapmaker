package net.hollowcube.mapmaker.command.invite;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class AcceptCommand extends CommandDsl {
    private final Argument<EntityFinder> targetArg = Argument.Entity("player")
            .singleEntity(true).onlyPlayers(true);

    private final PlayerInviteService inviteService;

    @Inject
    public AcceptCommand(@NotNull PlayerInviteService inviteService) {
        super("accept");
        this.inviteService = inviteService;

        category = CommandCategory.SOCIAL;

        addSyntax(playerOnly(this::handleDefaultAccept));
        addSyntax(playerOnly(this::handleAccept), targetArg);
    }

    private void handleDefaultAccept(@NotNull Player player, @NotNull CommandContext context) {
        inviteService.accept(player);
    }

    private void handleAccept(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(context.getRaw(targetArg))));
            return;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        inviteService.accept(player, target);
    }
}
