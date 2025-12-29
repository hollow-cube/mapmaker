package net.hollowcube.mapmaker.command.relationship;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.CoreCommandCondition;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnblockCommand extends CommandDsl {
    private final Argument<String> targetArg;

    private final PlayerService playerService;

    public UnblockCommand(@NotNull PlayerService playerService) {
        super("unblock");
        this.playerService = playerService;
        this.category = CommandCategories.SOCIAL;
        this.description = "Unblocks a player";
        this.examples = List.of("/unblock SethPRG");

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService).description("The player to unblock");

        this.setCondition(CoreCommandCondition.playerFeature(RelationshipFeatureFlag.FLAG));

        this.addSyntax(playerOnly(this::handleExec), this.targetArg);
    }

    private void handleExec(@NotNull Player player, @NotNull CommandContext context) {
        var targetId = context.get(this.targetArg);
        if (targetId == null) return;
        if (targetId.equals(player.getUuid().toString())) {
            player.sendMessage(Component.translatable("command.unblock.self"));
            return;
        }

        var targetRaw = context.getRaw(this.targetArg);

        try {
            this.playerService.unblockPlayer(player.getUuid().toString(), targetId);
            player.sendMessage(Component.translatable("command.unblock.success", Component.text(targetRaw)));
        } catch (PlayerService.NotFoundError ex) {
            player.sendMessage(Component.translatable("command.unblock.not_blocked", Component.text(targetRaw)));
        }
    }
}
