package net.hollowcube.map.command;

import net.hollowcube.map.invites.PlayerInviteService;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class AcceptInviteCommand extends Command {
    public AcceptInviteCommand() {
        super("accept");
        setDefaultExecutor(((sender, context) -> sender.sendMessage("Usage: /accept <player>")));
        addSyntax(this::accept, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void accept(@NotNull CommandSender sender, @NotNull CommandContext context) {
        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage("You can't accept yourself!");
            return;
        }

        if (target == null) {
            sender.sendMessage("That player is not online!");
            return;
        }

        PlayerInviteService.acceptInvite(target, (Player) sender);
    }
}
