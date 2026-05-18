package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class PHistoryCommand extends CommandDsl {
    private final Argument<String> playerArg;
    private final Argument<PunishmentType> typeArg = Argument.Enum("type", PunishmentType.class)
        .description("The type of punishment to check");

    private final PlayerClient players;
    private final PunishmentService punishmentService;

    public PHistoryCommand(
        @NotNull PlayerClient players,
        @NotNull PunishmentService punishmentService
    ) {
        super("phistory");
        this.players = players;
        this.punishmentService = punishmentService;

        category = CommandCategories.STAFF;
        description = "Check the punishment history of a player";
        playerArg = CoreArgument.AnyPlayerId("player", players)
            .description("The player to check the history of");

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::showPlayerHistory), playerArg);
        addSyntax(playerOnly(this::showPlayerHistory), playerArg, typeArg);
    }

    private void showPlayerHistory(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(playerArg);
        var type = context.get(typeArg);

        if (target == null) {
            player.sendMessage("Unknown player: " + context.getRaw(playerArg));
            return;
        }

        var punishments = new ArrayList<>(punishmentService.getPunishments(target, null, type));
        punishments.sort(Comparator.comparingLong(p -> p.createdAt().toEpochMilli()));
        if (punishments.isEmpty()) {
            player.sendMessage(Component.translatable("punishment.history.none"));
            return;
        }

        var targetDisplayName = players.getDisplayName(target).build();
        var builder = Component.text();
        builder.append(Component.translatable("punishment.history.header", targetDisplayName, Component.text(punishments.size())));

        for (var punishment : punishments) {
            builder.appendNewline();
            builder.append(Component.text(FontUtil.rewrite("small", punishment.type().name().toLowerCase(Locale.ROOT))));
            builder.append(Component.text(" » "));

            if (punishment.ladderId() != null) {
                builder.append(Component.text(punishment.ladderId())
                    .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text(punishment.comment()))));
            } else {
                builder.append(Component.text("'" + punishment.comment() + "'"));
            }

            builder.append(Component.text(" by "));
            builder.append(players.getDisplayName(punishment.executorId()).build());

            if (punishment.revokedAt() != null) {
                builder.append(Component.text(" ("));
                builder.append(Component.text("revoked").hoverEvent(HoverEvent.showText(
                    Component.text(Objects.requireNonNullElse(punishment.revokedReason(), "no reason given")))));
                builder.append(Component.text(" by "));
                builder.append(OpUtils.mapOr(punishment.revokedBy(), it -> players.getDisplayName(it).build(), Component.text("Unknown")));
                builder.append(Component.text(" " + NumberUtil.formatTimeSince(punishment.revokedAt())));
                builder.append(Component.text(" ago)"));
            } else if (punishment.expiresAt() != null) {
                if (punishment.expiresAt().isBefore(Instant.now())) {
                    builder.append(Component.text(" (expired "));
                    builder.append(Component.text(NumberUtil.formatTimeSince(punishment.expiresAt())));
                    builder.append(Component.text(" ago"));
                } else {
                    builder.append(Component.text(" (expires in "));
                    builder.append(Component.text(NumberUtil.formatTimeUntil(punishment.expiresAt())));
                }
                builder.append(Component.text(")"));
            } else if (punishment.type() != PunishmentType.KICK) {
                builder.append(Component.text(" (permanent)"));
            }
        }

        player.sendMessage(builder);
    }
}
