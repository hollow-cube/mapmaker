package net.hollowcube.map.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class TeleportCommand extends BaseMapCommand {

    public TeleportCommand() {
        super(true, "teleport", "tp");
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /teleport <player>"));
        addSyntax(this::teleport, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void teleport(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("command.generic.player_only"));
            return;
        }

        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        if (target == sender) {
            sender.sendMessage("You can't teleport to yourself!");
        }

        if (target == null) {
            sender.sendMessage("That player is not online or doesn't exist!");
            return;
        }

        var instance = player.getInstance();

        if (instance.getPlayers().contains(target)) {
            ((Player) sender).teleport(target.getPosition());
            sender.sendMessage("Teleported to " + target.getUsername() + ".");
            target.sendMessage(player.getUsername() + " has teleported to you.");
        } else {
            sender.sendMessage("That player isn't in your current build world!");
        }
    }
}