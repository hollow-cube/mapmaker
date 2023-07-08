package net.hollowcube.map.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InviteCommand extends BaseMapCommand {
    private Player inviter;
    private Player invitee;
    public InviteCommand() {
        super(false, "invite");
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


    }

    @Nullable
    private static Boolean getInviteResponse(Player inviter, Player invitee) {

    }

    private static void tick() {
        if (getInviteResponse())
    }
}
