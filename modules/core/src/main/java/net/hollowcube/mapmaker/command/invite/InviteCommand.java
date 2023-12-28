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

public class InviteCommand extends Command {
    private final Argument<EntityFinder> targetArg = Argument.Opt(Argument.Entity("player")
            .singleEntity(true).onlyPlayers(true));

    private final PlayerInviteService inviteService;

    public InviteCommand(@NotNull PlayerInviteService inviteService) {
        super("invite");
        this.inviteService = inviteService;

        category = CommandCategory.SOCIAL;

        addSyntax(playerOnly(this::handleInvite), targetArg);
    }

    private void handleInvite(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        String playerName = context.getRaw(targetArg);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player_offline", Component.text(playerName)));
            return;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }
        if (playerName.length() > 16 || playerName.length() < 3) {
            player.sendMessage(Component.text("generic.player_name_length"));
            return;
        }

        inviteService.registerInvite(player, target);
    }
}
