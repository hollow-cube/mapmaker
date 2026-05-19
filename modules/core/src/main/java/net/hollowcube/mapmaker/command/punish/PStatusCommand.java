package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class PStatusCommand extends CommandDsl {
    private final Argument<String> playerArg;

    private final PlayerClient players;
    private final PunishmentService punishmentService;

    public PStatusCommand(@NotNull PlayerClient players, @NotNull PunishmentService punishmentService) {
        super("pstatus");
        this.players = players;
        this.punishmentService = punishmentService;

        category = CommandCategories.STAFF;
        description = "Check the punishment status of a player";
        playerArg = CoreArgument.AnyPlayerId("player", players)
            .description("The player to check the status of");

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::showPlayerStatus), playerArg);
    }

    private void showPlayerStatus(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(playerArg);
        if (target == null) {
            player.sendMessage("Unknown player: " + context.getRaw(playerArg));
            return;
        }

        var targetDisplayName = players.getDisplayName(target).build();
        player.sendMessage(Component.translatable("punishment.status.header", targetDisplayName));

        var ban = punishmentService.getActivePunishment(target, PunishmentType.BAN);
        player.sendMessage(Component.translatable("punishment.status.ban.entry", buildStatusMessage(ban)));

        var mute = punishmentService.getActivePunishment(target, PunishmentType.MUTE);
        player.sendMessage(Component.translatable("punishment.status.mute.entry", buildStatusMessage(mute)));
    }

    private Component buildStatusMessage(@Nullable Punishment punishment) {
        if (punishment == null) return Component.translatable("punishment.status.none");

        return Component.translatable("punishment.status.entry", List.of(
            players.getDisplayName(punishment.executorId()).build(),
            Component.text(Objects.requireNonNullElse(punishment.ladderId(), "none")),
            Component.text(punishment.comment()),
            Component.text(NumberUtil.formatTimeSince(punishment.createdAt())),
            punishment.expiresAt() == null ? Component.translatable("punishment.status.duration.permanent")
                : Component.translatable("punishment.status.duration.relative", Component.text(NumberUtil.formatTimeUntil(punishment.expiresAt())))
        ));
    }

}
