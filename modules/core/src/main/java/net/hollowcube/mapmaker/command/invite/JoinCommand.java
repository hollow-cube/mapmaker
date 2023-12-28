package net.hollowcube.mapmaker.command.invite;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.world.KindaBadThingToFix;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class JoinCommand extends Command {
    private final Argument<EntityFinder> targetArg = Argument.Opt(Argument.Entity("player")
            .singleEntity(true).onlyPlayers(true));

    private final PlayerInviteService inviteService;
    private final PermManager permManager;

    public JoinCommand(@NotNull PlayerInviteService inviteService, @NotNull PermManager permManager) {
        super("join");
        this.inviteService = inviteService;
        this.permManager = permManager;

        category = CommandCategory.SOCIAL;

        addSyntax(playerOnly(this::handleJoin), targetArg);
    }

    private void handleJoin(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player_offline", Component.text(context.getRaw(targetArg))));
            return;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        var targetMap = KindaBadThingToFix.getMapFromCurrentWorld(target);
        var senderMap = KindaBadThingToFix.getMapFromCurrentWorld(player);

        if (targetMap == null) {
            player.sendMessage(Component.translatable("command.join.hub", Component.translatable(target.getUsername())));
        } else if (targetMap == senderMap) {
            player.sendMessage(Component.translatable("command.join.same_map", Component.translatable(target.getUsername())));
        } else if (targetMap.isPublished() || permManager.hasPlatformPermission(player, PlatformPerm.MAP_ADMIN)) {
            inviteService.join(player, target);
        } else if (!targetMap.isPublished()) {
            player.sendMessage(Component.translatable("command.join.building", Component.translatable(target.getUsername())));
        } else {
            player.sendMessage(Component.translatable("generic.unknown_error"));
        }
    }
}
