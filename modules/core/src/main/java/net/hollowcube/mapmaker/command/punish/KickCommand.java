package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;

import java.util.UUID;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class KickCommand extends CommandDsl {

    private final PunishmentService service;

    private final Argument<String> targetArgument;
    private final Argument<String> reasonArgument = Argument.GreedyString("reason");

    public KickCommand(PunishmentService service, SessionManager sessionManager) {
        super("kick");
        this.service = service;
        this.targetArgument = CoreArgument.AnyOnlinePlayer("target", sessionManager);

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        this.addSyntax(playerOnly(this::execute), this.targetArgument, this.reasonArgument);
    }

    private void execute(Player sender, CommandContext context) {
        var target = context.get(this.targetArgument);
        var reason = context.get(this.reasonArgument);

        if (target == null || reason == null) {
            return;
        }

        var executorId = sender.getUuid();
        var targetId = UUID.fromString(target);

        this.service.createPunishment(targetId, executorId, PunishmentType.KICK, reason, null);
        sender.sendMessage("Kicked " + target + " for " + reason);
    }
}
