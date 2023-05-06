package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GeneralCommands {
    private GeneralCommands() {
    }

    public static final class Undo extends Command {

        public Undo() {
            super("/undo");

            setDefaultExecutor(this::undoblah);
        }

        private void undoblah(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player))
                throw new UnsupportedOperationException("only implemented for players");

            var session = LocalSession.forPlayer(player);
            int undone = session.undo(1);
            player.sendMessage("Undone " + undone + " actions");
        }

    }

    public static final class Redo extends Command {

        public Redo() {
            super("/redo");

            setDefaultExecutor(this::redoblah);
        }

        private void redoblah(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player))
                throw new UnsupportedOperationException("only implemented for players");

            var session = LocalSession.forPlayer(player);
            int undone = session.redo(1);
            player.sendMessage("Redone " + undone + " actions");
        }

    }

    public static final class ClearHistory extends Command {

        public ClearHistory() {
            super("/clearhistory");

            setDefaultExecutor(this::clearHistory);
        }

        private void clearHistory(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player))
                throw new UnsupportedOperationException("only implemented for players");

            var session = LocalSession.forPlayer(player);
            session.clearHistory();
            player.sendMessage("Cleared history");
        }

    }
}
