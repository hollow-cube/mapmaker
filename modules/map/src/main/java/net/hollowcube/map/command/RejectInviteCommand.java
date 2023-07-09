package net.hollowcube.map.command;

import net.hollowcube.map.invites.PlayerInviteService;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class RejectInviteCommand extends Command {
    public RejectInviteCommand() {
        super("reject");
        setDefaultExecutor(((sender, context) -> sender.sendMessage("Usage: /reject <player>")));
        addSyntax(this::reject, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void reject(@NotNull CommandSender sender, @NotNull CommandContext context) {
        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage("You can't reject yourself!");
            return;
        }

        if (target == null) {
            sender.sendMessage("That player is not online!");
            return;
        }

        PlayerInviteService.rejectInvite(target, (Player) sender);
    }
}
