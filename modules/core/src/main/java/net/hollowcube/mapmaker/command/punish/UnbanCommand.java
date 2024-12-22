package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UnbanCommand extends CommandDsl {
    private final Argument<String> targetArgument;
    private final Argument<String> reasonArgument = Argument.GreedyString("reason")
            .description("The reason for the unban");

    private final PunishmentService punishmentService;

    public UnbanCommand(
            @NotNull PunishmentService punishmentService,
            @NotNull PlayerService playerService,
            @NotNull PermManager permManager
    ) {
        super("unban");
        this.punishmentService = punishmentService;

        category = CommandCategories.STAFF;
        description = "Unban a player from the server";
        this.targetArgument = CoreArgument.AnyPlayerId("target", playerService)
                .description("The player to unban");

        setCondition(permManager.createPlatformCondition2(PlatformPerm.BAN_PLAYER));
        this.addSyntax(playerOnly(this::execute), this.targetArgument, this.reasonArgument);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(this.targetArgument);
        if (target == null) return;

        var targetId = UUID.fromString(target);
        var reason = context.get(this.reasonArgument);

        this.punishmentService.revokePunishment(targetId, PunishmentType.BAN, player.getUuid(), reason);
        player.sendMessage(Component.text("Unbanned " + target + " for " + reason));
    }
}
