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
import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class PStatusCommand extends CommandDsl {
    private final Argument<String> playerArg;

    private final PlayerService playerService;
    private final PunishmentService punishmentService;

    public PStatusCommand(
            @NotNull PlayerService playerService,
            @NotNull PunishmentService punishmentService,
            @NotNull PermManager permManager
    ) {
        super("pstatus");
        this.playerService = playerService;
        this.punishmentService = punishmentService;

        category = CommandCategories.STAFF;
        description = "Check the punishment status of a player";
        playerArg = CoreArgument.AnyPlayerId("player", playerService)
                .description("The player to check the status of");

        setCondition(permManager.createPlatformCondition2(PlatformPerm.VIEW_PUNISHMENTS));
        addSyntax(playerOnly(this::showPlayerStatus), playerArg);
    }

    private void showPlayerStatus(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(playerArg);

        var targetDisplayName = playerService.getPlayerDisplayName2(target).build();
        player.sendMessage(Component.translatable("punishment.status.header", targetDisplayName));

        var ban = punishmentService.getActivePunishment(target, PunishmentType.BAN);
        player.sendMessage(Component.translatable("punishment.status.ban.entry", buildStatusMessage(ban)));

        var mute = punishmentService.getActivePunishment(target, PunishmentType.MUTE);
        player.sendMessage(Component.translatable("punishment.status.mute.entry", buildStatusMessage(mute)));
    }

    private Component buildStatusMessage(@Nullable Punishment punishment) {
        if (punishment == null) return Component.translatable("punishment.status.none");

        return Component.translatable("punishment.status.entry", List.of(
                playerService.getPlayerDisplayName2(punishment.executorId()).build(),
                Component.text(Objects.requireNonNullElse(punishment.ladderId(), "none")),
                Component.text(punishment.comment()),
                Component.text(NumberUtil.formatTimeSince(punishment.createdAt())),
                punishment.expiresAt() == null ? Component.translatable("punishment.status.duration.permanent")
                        : Component.translatable("punishment.status.duration.relative", Component.text(NumberUtil.formatTimeUntil(punishment.expiresAt())))
        ));
    }

}
