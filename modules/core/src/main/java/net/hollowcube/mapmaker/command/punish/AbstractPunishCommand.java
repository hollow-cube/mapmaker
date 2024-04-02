package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

abstract class AbstractPunishCommand extends CommandDsl {

    protected final PunishmentService service;
    private final PunishmentType type;

    private final Argument<String> targetArgument;
    private final Argument<PunishmentLadder> ladderArgument;
    private final Argument<String> commentArgument = Argument.GreedyString("comment");

    AbstractPunishCommand(@NotNull String name, @NotNull PunishmentType type, @NotNull PunishmentService service,
                          @NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super(name);

        this.service = service;
        this.type = type;

        this.targetArgument = CoreArgument.AnyPlayerId("target", playerService);
        this.ladderArgument = Argument.Word("ladder").map(
                (sender, raw) -> {
                    try {
                        return new ParseResult.Success<>(this.service.getLadderById(raw));
                    } catch (Exception exception) {
                        return new ParseResult.Partial<>();
                    }
                },
                (sender, raw, suggestion) -> {
                    for (var ladder : this.service.searchLadders(raw, type)) {
                        suggestion.add(ladder.id());
                    }
                }
        );

        setCondition(permManager.createPlatformCondition2(type == PunishmentType.BAN ? PlatformPerm.BAN_PLAYER : PlatformPerm.MUTE_PLAYER));
        this.addSyntax(playerOnly(this::execute), this.targetArgument, this.ladderArgument, this.commentArgument);
    }

    private void execute(@NotNull Player sender, @NotNull CommandContext context) {
        var target = context.get(this.targetArgument);
        var ladder = context.get(this.ladderArgument);
        var comment = context.get(this.commentArgument);

        if (target == null || ladder == null || comment == null) {
            return;
        }

        var executorId = sender.getUuid();
        var targetId = UUID.fromString(target);

        this.service.createPunishment(targetId, executorId, this.type, comment, ladder.id());
        sender.sendMessage(Component.text("Create Punishment. Type: " + this.type + ", Target: " + target + ", Ladder: " + ladder.id() + ", Comment: " + comment));
    }
}
