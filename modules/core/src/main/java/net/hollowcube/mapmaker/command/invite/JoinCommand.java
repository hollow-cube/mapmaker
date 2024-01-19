package net.hollowcube.mapmaker.command.invite;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class JoinCommand extends CommandDsl {
    private final Argument<String> targetArg;

    private final MapService mapService;
    private final SessionManager sessionManager;
    private final PermManager permManager;
    private final PlayerInviteService inviteService;

    @Inject
    public JoinCommand(
            @NotNull MapService mapService,
            @NotNull SessionManager sessionManager,
            @NotNull PermManager permManager,
            @NotNull PlayerInviteService inviteService
    ) {
        super("join");
        this.mapService = mapService;
        this.sessionManager = sessionManager;
        this.permManager = permManager;
        this.inviteService = inviteService;

        category = CommandCategory.SOCIAL;

        targetArg = CoreArgument.AnyOnlinePlayer("player", sessionManager);

        addSyntax(playerOnly(this::handleJoin), targetArg);
    }

    private void handleJoin(@NotNull Player player, @NotNull CommandContext context) {
        var targetId = context.get(targetArg);
        if (targetId == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(context.getRaw(targetArg))));
            return;
        }

        var playerId = PlayerDataV2.fromPlayer(player).id();
        if (playerId.equals(targetId)) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        var targetData = Objects.requireNonNull(sessionManager.getSession(targetId)); //todo AnyOnlinePlayer should just become AnySession and return the session.
        var targetMap = MiscFunctionality.getCurrentMap(sessionManager, mapService, targetId);
        var senderMap = MiscFunctionality.getCurrentMap(sessionManager, mapService, player);

        if (targetMap == null) {
            player.sendMessage(Component.translatable("command.join.hub", Component.translatable(targetData.username())));
        } else if (targetMap == senderMap) {
            player.sendMessage(Component.translatable("command.join.same_map", Component.translatable(targetData.username())));
        } else if (targetMap.isPublished() || permManager.hasPlatformPermission(player, PlatformPerm.MAP_ADMIN)) {
//            inviteService.join(player, targetId);
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (!targetMap.isPublished()) {
            player.sendMessage(Component.translatable("command.join.building", Component.translatable(targetData.username())));
        } else {
            player.sendMessage(Component.translatable("generic.unknown_error"));
        }
    }
}
