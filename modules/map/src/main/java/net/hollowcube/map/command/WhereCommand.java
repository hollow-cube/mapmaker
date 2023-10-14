package net.hollowcube.map.command;

import net.hollowcube.map.world.MapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class WhereCommand extends Command {
    public WhereCommand() {
        super("where", "whereis", "find");
        setDefaultExecutor((sender, context) -> sender.sendMessage(Component.translatable("command.where.usage")));
        addSyntax(this::where, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void where(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
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

        var targetMap = MapWorld.forPlayerOptional(target);
        var senderMap = MapWorld.forPlayerOptional(player);

        if (targetMap == null) {
            sender.sendMessage(Component.translatable("generic.in_hub", Component.translatable(target.getUsername())));
        } else if (targetMap == senderMap) {
            sender.sendMessage(Component.translatable("generic.same_map", Component.translatable(target.getUsername())));
        } else if (targetMap.map().isPublished()) {
            sender.sendMessage(Component.translatable("command.where.playing", Component.translatable(target.getUsername()), Component.translatable(targetMap.map().name())));
        } else if (!targetMap.map().isPublished()) {
            sender.sendMessage(Component.translatable("command.where.building", Component.translatable(target.getUsername())));
        } else {
            sender.sendMessage(Component.translatable("generic.unknown_error"));
        }
    }
}
