package net.hollowcube.mapmaker.command.relationship;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class UnblockCommand extends CommandDsl {
    private final Argument<String> targetArg;

    private final SocialService social;

    public UnblockCommand(@NotNull PlayerService players, @NotNull SocialService social) {
        super("unblock");
        this.social = social;
        this.category = CommandCategories.SOCIAL;
        this.description = "Unblocks a player";
        this.examples = List.of("/unblock SethPRG");

        this.targetArg = CoreArgument.AnyPlayerId("target", players).description("The player to unblock");

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

        if (this.social.unblock(player.getUuid(), UUID.fromString(targetId))) {
            player.sendMessage(Component.translatable("command.unblock.success", Component.text(targetRaw)));
        } else {
            player.sendMessage(Component.translatable("command.unblock.not_blocked", Component.text(targetRaw)));
        }
    }
}
