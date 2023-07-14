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

public class AcceptCommand extends Command {
    public AcceptCommand() {
        super("accept");
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /accept <player>"));
        addSyntax(this::accept, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void accept(@NotNull CommandSender sender, @NotNull CommandContext context) {
        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }

        if (target == null) {
            sender.sendMessage(Component.translatable("generic.player_offline", Component.text(target.toString()))); //uh, maybe null, but hopefully it's just passing the target name lol
            return;
        }

        PlayerInviteService.accept((Player) sender, target);
    }
}
