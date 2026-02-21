package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class PHelpCommand extends CommandDsl {
    private final Argument<PunishmentType> typeArg = Argument.Enum("type", PunishmentType.class)
        .description("Show only entries for the given type");

    private final PunishmentService punishmentService;

    public PHelpCommand(@NotNull PunishmentService punishmentService) {
        super("phelp");
        this.punishmentService = punishmentService;

        category = CommandCategories.STAFF;
        description = "Show information about the punishment ladders";

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::showLadderInfo));
        addSyntax(playerOnly(this::showLadderInfo), typeArg);
    }

    private void showLadderInfo(@NotNull Player player, @NotNull CommandContext context) {
        var typeFilter = context.get(typeArg);

        var builder = Component.text();
        builder.append(LanguageProviderV2.translateMultiMerged("punishment.help.header", List.of()));

        var ladders = new ArrayList<>(punishmentService.getAllLadders());
        ladders.sort(Comparator.comparing(PunishmentLadder::type));
        for (var ladder : ladders) {
            if (typeFilter != null && ladder.type() != typeFilter)
                continue;

            // Name of ladder
            builder.appendNewline();
            builder.append(Component.translatable("punishment.help.ladder.name", List.of(
                Component.text(FontUtil.rewrite("small", ladder.type().name().toLowerCase(Locale.ROOT))),
                Component.translatable("punishment.ladder." + ladder.id()))
            ));

            // Duration track
            var track = Component.text();
            for (int i = 0; i < ladder.entries().size(); i++) {
                if (i > 0) track.append(Component.translatable("punishment.help.ladder.separator"));
                track.append(Component.translatable("punishment.help.ladder.entry",
                    Component.text(formatDuration(ladder.entries().get(i).duration()))));
            }
            builder.appendSpace();
            builder.append(Component.translatable("punishment.help.ladder.entries", track));

            // Reasons
            for (var reason : ladder.reasons()) {
                var aliases = Component.text();
                aliases.append(Component.text(reason.id()));
                for (var alias : reason.aliases()) {
                    aliases.append(Component.text(", " + alias));
                }

                builder.appendNewline();
                builder.append(Component.text("- "));
                builder.append(Component.translatable("punishment.reason." + reason.id()));
                builder.append(Component.text(" (aka: "));
                builder.append(aliases);
                builder.append(Component.text(")"));
            }
        }

        player.sendMessage(builder);
    }

    private static final long HOUR = 60 * 60;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;

    private static @NotNull String formatDuration(long duration) {
        if (duration < 0) {
            return "permanent";
        } else if (duration >= MONTH) {
            return (duration / MONTH) + "mo";
        } else if (duration >= WEEK) {
            return (duration / WEEK) + "w";
        } else if (duration >= DAY) {
            return (duration / DAY) + "d";
        } else if (duration >= HOUR) {
            return (duration / HOUR) + "h";
        } else {
            return "0h"; // default case if less than an hour
        }
    }
}
