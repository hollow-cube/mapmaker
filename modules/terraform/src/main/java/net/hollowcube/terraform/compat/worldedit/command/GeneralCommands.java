package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.compat.worldedit.util.WECommand;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Messages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GeneralCommands {

    public static final class Undo extends WECommand {
        private final Argument<Integer> times = Argument.Int("times").clamp(1, 50).defaultValue(1);

        public Undo() {
            super("/undo");

            addSyntax(playerOnly(this::undo));
            addSyntax(playerOnly(this::undo), times);
        }

        private void undo(@NotNull Player player, @NotNull CommandContext context) {
            int count = context.get(times);

            var session = LocalSession.forPlayer(player);
            int undone = session.undo(count);

            player.sendMessage(Messages.HISTORY_UNDO.with(undone));
        }

    }

    public static final class Redo extends WECommand {
        private final Argument<Integer> times = Argument.Int("times").clamp(1, 50).defaultValue(1);

        public Redo() {
            super("/redo");

            addSyntax(playerOnly(this::redo));
            addSyntax(playerOnly(this::redo), times);
        }

        private void redo(@NotNull Player player, @NotNull CommandContext context) {
            int count = context.get(times);

            var session = LocalSession.forPlayer(player);
            int undone = session.redo(count);

            player.sendMessage(Messages.HISTORY_REDO.with(undone));
        }

    }

    public static final class ClearHistory extends WECommand {

        public ClearHistory() {
            super("/clearhistory");

            addSyntax(playerOnly(this::handleClearHistory));
        }

        private void handleClearHistory(@NotNull Player player, @NotNull CommandContext context) {
            var session = LocalSession.forPlayer(player);
            session.clearHistory();

            player.sendMessage(Messages.HISTORY_CLEARED);
        }

    }

    public static final class GMask extends WECommand {

        public GMask() {
            super("/gmask");

            addSyntax(playerOnly(this::handleGMask));
        }

        private void handleGMask(@NotNull Player player, @NotNull CommandContext context) {
            player.sendMessage("todo //gmask");
        }

    }

    private GeneralCommands() {
    }
}
