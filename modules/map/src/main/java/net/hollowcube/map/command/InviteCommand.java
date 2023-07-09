package net.hollowcube.map.command;

import net.hollowcube.map.invites.PlayerInviteService;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InviteCommand extends Command {
    public InviteCommand() {
        super("invite");
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /invite <player>"));
        addSyntax(this::invite, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void invite(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("command.generic.player_only"));
            return;
        }

        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage("You can't invite yourself!");
            return;
        }

        if (target == null) {
            sender.sendMessage("That player is not online!");
            return;
        }

        PlayerInviteService.registerInvite((Player) sender, target);
    }
}
