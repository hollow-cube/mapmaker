package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.compat.worldedit.util.CommandUtil;
import net.hollowcube.terraform.session.Session;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HistoryCommands {

    public HistoryCommands(CommandManager commands) {
        commands.register(CommandUtil.singleSyntaxCommand("/undo", this::undo));
        commands.register(CommandUtil.singleSyntaxCommand("/redo", this::redo));
    }

    public void undo(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var session = Session.forPlayer(player);
        session.undo(player.getInstance())
                .thenAccept(unused -> player.sendMessage("Done!"))
                .exceptionally(e -> {
                    player.sendMessage("Error: " + e.getMessage());
                    return null;
                });
    }

    public void redo(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var session = Session.forPlayer(player);
        session.redo(player.getInstance())
                .thenAccept(unused -> player.sendMessage("Done!"))
                .exceptionally(e -> {
                    player.sendMessage("Error: " + e.getMessage());
                    return null;
                });
    }
}
