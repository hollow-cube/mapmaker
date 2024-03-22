package net.hollowcube.terraform.command.terraform;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.task.Task;
import net.hollowcube.terraform.util.Messages;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class TerraformCancelCommand extends CommandDsl {
    private final Argument<Task> taskArg = new ArgumentTask("task");

    public TerraformCancelCommand() {
        super("cancel");

        addSyntax(playerOnly(this::cancelTask), taskArg);
        addSyntax(playerOnly(this::cancelAllTasks), Argument.Word("all").with("all"));
    }

    private void cancelTask(@NotNull Player player, @NotNull CommandContext context) {
        var task = context.get(taskArg);

        var session = LocalSession.forPlayerOptional(player);
        if (session == null) {
            player.sendMessage(Messages.GENERIC_NO_LOCAL_SESSION);
            return;
        }

        var resultState = session.cancelTask(task);
        if (!resultState.isTerminal()) {
            player.sendMessage(Messages.TF_CANCEL_INVALID);
            return;
        }
        player.sendMessage(Messages.TF_CANCEL_SUCCESS.with(task));
    }

    private void cancelAllTasks(@NotNull Player player, @NotNull CommandContext context) {
        var session = LocalSession.forPlayerOptional(player);
        if (session == null) {
            player.sendMessage(Messages.GENERIC_NO_LOCAL_SESSION);
            return;
        }

        var tasks = session.tasks();
        if (tasks.isEmpty()) {
            player.sendMessage(Messages.TF_CANCEL_ALL_NONE);
            return;
        }

        tasks.forEach(session::cancelTask);
        player.sendMessage(Messages.TF_CANCEL_ALL_SUCCESS);
    }

    private static final class ArgumentTask extends Argument<Task> {
        ArgumentTask(String id) {
            super(id);
        }

        @Override
        public @NotNull ParseResult<Task> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
            var session = getSession(sender);
            if (session == null) return syntaxError();

            var word = reader.readWord(WordType.BRIGADIER).toLowerCase(Locale.ROOT);
            for (var task : session.tasks()) {
                if (word.equals(task.toString()))
                    return success(task);
            }

            return partial(); //todo need to give error message
        }

        @Override
        public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
            var session = getSession(sender);
            if (session == null) return;

            for (var task : session.tasks()) {
                if (task.toString().startsWith(raw))
                    suggestion.add(task.toString());
            }
        }

        private @Nullable LocalSession getSession(@NotNull CommandSender sender) {
            if (!(sender instanceof Player player)) return null;
            return LocalSession.forPlayerOptional(player);
        }
    }
}
