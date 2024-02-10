package net.hollowcube.mapmaker.command.punish;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.PunishmentType;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class KickCommand extends CommandDsl {

    private final PunishmentService service;

    private final Argument<String> targetArgument;
    private final Argument<String> reasonArgument = Argument.GreedyString("reason");

    @Inject
    public KickCommand(@NotNull PunishmentService service, @NotNull SessionManager sessionManager) {
        super("kick");
        this.service = service;
        this.targetArgument = CoreArgument.AnyOnlinePlayer("target", sessionManager);

        this.addSyntax(playerOnly(this::execute), this.targetArgument, this.reasonArgument);
    }

    private void execute(@NotNull Player sender, @NotNull CommandContext context) {
        var target = context.get(this.targetArgument);
        var reason = context.get(this.reasonArgument);

        if (target == null || reason == null) {
            return;
        }

        var executorId = sender.getUuid();
        var targetId = UUID.fromString(target);

        this.punish(executorId, targetId, reason);
    }

    private void punish(@NotNull UUID executorId, @NotNull UUID targetId, @NotNull String reason) {
        this.service.createPunishment(targetId, executorId, PunishmentType.KICK, reason, null);
    }
}
