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

public class RequestCommand extends Command {
    public RequestCommand() {
        super("request");
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /request <player>"));
        addSyntax(this::request, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void request(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("command.generic.player_only"));
            return;
        }

        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage("You can't request to join yourself!");
            return;
        }

        if (target == null) {
            sender.sendMessage("That player is not online!");
            return;
        }

        PlayerInviteService.registerRequest((Player) sender, target);
    }
}
