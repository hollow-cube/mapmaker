package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.UUID;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class UnmuteCommand extends CommandDsl {
    private final Argument<String> targetArgument;
    private final Argument<String> reasonArgument = Argument.GreedyString("reason")
        .description("The reason for the unmute");

    private final PunishmentService punishmentService;

    public UnmuteCommand(PunishmentService punishmentService, PlayerService playerService) {
        super("unmute");
        this.punishmentService = punishmentService;

        category = CommandCategories.STAFF;
        description = "Unmute a player from the server";
        this.targetArgument = CoreArgument.AnyPlayerId("target", playerService);

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        this.addSyntax(playerOnly(this::execute), this.targetArgument, this.reasonArgument);
    }

    private void execute(Player player, CommandContext context) {
        var target = context.get(this.targetArgument);
        if (target == null) return;

        var targetId = UUID.fromString(target);
        var reason = context.get(this.reasonArgument);

        this.punishmentService.revokePunishment(targetId, PunishmentType.MUTE, player.getUuid(), reason);
        player.sendMessage(Component.text("Unmuted " + target + " for " + reason));
    }
}
