package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

abstract class AbstractPunishCommand extends CommandDsl {

    protected final PunishmentService service;
    private final PunishmentType type;

    private final Argument<String> targetArgument;
    private final Argument<PunishmentLadder> ladderArgument;
    private final Argument<String> commentArgument = Argument.GreedyString("comment");

    AbstractPunishCommand(@NotNull String name, @NotNull PunishmentType type, @NotNull PunishmentService service,
                          @NotNull PlayerClient players) {
        super(name);

        this.service = service;
        this.type = type;

        var allLaddersByName = new HashMap<String, PunishmentLadder>();
        for (var ladder : this.service.getLaddersByType(type)) {
            allLaddersByName.put(ladder.id().toLowerCase(Locale.ROOT), ladder);
            for (var reason : ladder.reasons()) {
                allLaddersByName.put(reason.id().toLowerCase(Locale.ROOT), ladder);
                for (var alias : reason.aliases()) {
                    allLaddersByName.put(alias.toLowerCase(Locale.ROOT), ladder);
                }
            }
        }
        var allLadderNames = allLaddersByName.keySet().stream().sorted().toList();

        this.targetArgument = CoreArgument.AnyPlayerId("target", players);
        this.ladderArgument = Argument.Word("ladder").map(
            (sender, raw) -> {
                try {
                    return new ParseResult.Success<>(allLaddersByName.get(raw.toLowerCase(Locale.ROOT)));
                } catch (Exception exception) {
                    return new ParseResult.Partial<>();
                }
            },
            (sender, raw, suggestion) -> {
                raw = raw.toLowerCase(Locale.ROOT);
                for (var ladder : allLadderNames) {
                    if (ladder.startsWith(raw)) {
                        suggestion.add(ladder);
                    }
                }
            }
        );

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        this.addSyntax(playerOnly(this::execute), this.targetArgument, this.ladderArgument);
        this.addSyntax(playerOnly(this::execute), this.targetArgument, this.ladderArgument, this.commentArgument);
    }

    private void execute(@NotNull Player sender, @NotNull CommandContext context) {
        try {
            var target = context.get(this.targetArgument);
            var ladder = context.get(this.ladderArgument);
            var comment = context.get(this.commentArgument);

            if (target == null) {
                sender.sendMessage("Unknown player: " + context.getRaw(targetArgument));
                return;
            }
            if (ladder == null) {
                sender.sendMessage("Unknown ladder: " + context.getRaw(ladderArgument));
                return;
            }

            var executorId = sender.getUuid();
            var targetId = UUID.fromString(target);

            this.service.createPunishment(targetId, executorId, this.type, comment, context.getRaw(ladderArgument));
        } catch (Exception e) {
            sender.sendMessage("An error occurred while executing the command.");
            ExceptionReporter.reportException(e, sender);
        }
    }
}
