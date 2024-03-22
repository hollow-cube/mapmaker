package net.hollowcube.terraform.command.terraform;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Format;
import net.hollowcube.terraform.util.Messages;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

public class TerraformQueueCommand extends CommandDsl {
    private final Argument<EntityFinder> playerArg = Argument.Entity("player")
            .onlyPlayers(true).singleEntity(true)
            .description("The player to view the queue of");

    public TerraformQueueCommand() {
        super("queue");
        description = "View the current task queue";

        addSyntax(playerOnly(this::showQueueSelf));
        addSyntax(playerOnly(this::showQueueOther), playerArg); //todo permission


        // tf queue: show your current queue
        // tf queue <player>: show another players queue (permissioned)
        // tf cancel: cancel a task by its id
        // tf cancel all: cancel all tasks
    }

    private void showQueueSelf(@NotNull Player player, @NotNull CommandContext context) {
        var session = LocalSession.forPlayerOptional(player);
        if (session == null) {
            player.sendMessage(Messages.GENERIC_NO_LOCAL_SESSION);
            return;
        }

        printTaskQueue(player, session);
    }

    private void showQueueOther(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(playerArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage(Messages.TF_QUEUE_TARGET_NOT_FOUND.with(context.getRaw(playerArg)));
            return;
        }

        var session = LocalSession.forPlayerOptional(target);
        if (session == null) {
            player.sendMessage(Messages.GENERIC_NO_LOCAL_SESSION_OTHER.with(target.getUsername()));
            return;
        }

        printTaskQueue(player, session);
    }

    private void printTaskQueue(@NotNull Player player, @NotNull LocalSession session) {
        var tasks = session.tasks();
        if (tasks.isEmpty()) {
            player.sendMessage(Messages.TF_QUEUE_EMPTY);
            return;
        }

        player.sendMessage(Messages.TF_QUEUE_HEADER.with(tasks.size()));
        for (var task : tasks) {
            player.sendMessage(Messages.TF_QUEUE_ENTRY.with(
                    task, task.state(),
                    Format.formatTimeSince(task.created())
            ));
        }
    }
}
