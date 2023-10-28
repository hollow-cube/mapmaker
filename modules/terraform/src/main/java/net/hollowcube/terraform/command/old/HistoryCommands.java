package net.hollowcube.terraform.command.old;

import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HistoryCommands {
    private HistoryCommands() {
    }

    public static final class Undo extends Command {
        private final Argument<Integer> countArg = ArgumentType.Integer("count").min(1).setDefaultValue(1);

        public Undo(@Nullable CommandCondition condition) {
            super("undo", "tf:undo");
            setCondition(condition);

            setDefaultExecutor(this::handleUndo);
            addSyntax(this::handleUndo, countArg);
        }

        private void handleUndo(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            int count = 1;
            if (context.has(countArg)) {
                count = context.get(countArg);
            }

            var session = LocalSession.forPlayer(player);
            int undone = session.undo(count);
            player.sendMessage(Component.translatable("command.terraform.history.undo", Component.text(undone)));
        }
    }

    public static final class Redo extends Command {
        private final Argument<Integer> countArg = ArgumentType.Integer("count").min(1).setDefaultValue(1);

        public Redo(@Nullable CommandCondition condition) {
            super("redo", "tf:redo");
            setCondition(condition);

            setDefaultExecutor(this::handleUndo);
            addSyntax(this::handleUndo, countArg);
        }

        private void handleUndo(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            int count = 1;
            if (context.has(countArg)) {
                count = context.get(countArg);
            }

            var session = LocalSession.forPlayer(player);
            int redone = session.redo(count);
            player.sendMessage(Component.translatable("command.terraform.history.redo", Component.text(redone)));
        }
    }

    public static final class ClearHistory extends Command {

        public ClearHistory(@Nullable CommandCondition condition) {
            super("clearhistory", "tf:clearhistory");
            setCondition(condition);

            setDefaultExecutor(this::handleClearHistory);
        }

        private void handleClearHistory(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("generic.players_only"));
                return;
            }

            var session = LocalSession.forPlayer(player);
            session.clearHistory();
            player.sendMessage(Component.translatable("command.terraform.history.cleared"));
        }
    }
}
