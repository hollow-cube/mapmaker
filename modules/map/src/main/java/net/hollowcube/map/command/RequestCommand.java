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
        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.translatable("command.request.usage")));
        addSyntax(this::request, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void request(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.translatable("command.generic.player_only"));
            return;
        }

        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        if (target == null) {
            sender.sendMessage(Component.translatable("generic.player_offline", Component.text(context.get("player").toString())));
            return;
        }

        PlayerInviteService.registerRequest((Player) sender, target);
    }
}
