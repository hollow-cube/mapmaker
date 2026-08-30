package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandExecutedEvent;
import net.hollowcube.command.CommandResult;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.ipc.chat.ChatService;
import net.hollowcube.ipc.chat.CommandExecution;
import net.hollowcube.ipc.chat.CommandOutcome;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.player.PlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/// Sends every command a player ran to the api.
///
/// On the command's own thread, which by then has answered the player and has nothing left to do,
/// so the round trip costs them nothing. An api that refuses the row loses it: a command log is not
/// worth a queue, a retry, or holding anything up.
public final class CommandLogReporter {
    private static final Logger logger = LoggerFactory.getLogger(CommandLogReporter.class);

    /// A stack trace pasted whole is not worth a column; the type and message are the useful part.
    private static final int MAX_ERROR = 1024;

    private final ChatService chat;
    /// Read per command rather than captured, because the remote commands are reloaded while the
    /// server runs; what it answers is an immutable snapshot.
    private final Supplier<Set<String>> remoteCommands;

    public CommandLogReporter(@NotNull ChatService chat, @NotNull Supplier<Set<String>> remoteCommands) {
        this.chat = chat;
        this.remoteCommands = remoteCommands;
    }

    public void onCommandExecuted(@NotNull CommandExecutedEvent event) {
        var player = event.player();
        var world = MapWorld.forPlayer(player);
        var run = new CommandExecution(
            Instant.now(),
            PlayerData.fromPlayer(player).id(),
            ServerRuntime.getRuntime().hostname(),
            world == null ? null : world.map().id(),
            world == null ? null : world.worldId(),
            event.input(),
            remoteCommands.get().contains(name(event.input())),
            outcome(event.result()),
            error(event.result()),
            (int) TimeUnit.NANOSECONDS.toMillis(event.durationNanos()));

        try {
            chat.logCommand(run);
        } catch (IpcException e) {
            logger.warn("dropped a command log row: {}", e.getMessage());
        }
    }

    /// The command itself, which is what a remote command is registered under.
    private static String name(String input) {
        int space = input.indexOf(' ');
        return space < 0 ? input : input.substring(0, space);
    }

    private static CommandOutcome outcome(CommandResult result) {
        return switch (result) {
            case CommandResult.Success _ -> CommandOutcome.SUCCESS;
            case CommandResult.Denied _ -> CommandOutcome.DENIED;
            case CommandResult.NotFound _ -> CommandOutcome.NOT_FOUND;
            case CommandResult.SyntaxError _ -> CommandOutcome.SYNTAX_ERROR;
            case CommandResult.ExecutionError _ -> CommandOutcome.EXECUTION_ERROR;
        };
    }

    private static @Nullable String error(CommandResult result) {
        return switch (result) {
            case CommandResult.SyntaxError syntaxError -> truncate(syntaxError.message());
            case CommandResult.ExecutionError executionError -> truncate(executionError.cause().toString());
            case CommandResult.Success _, CommandResult.Denied _, CommandResult.NotFound _ -> null;
        };
    }

    private static @Nullable String truncate(@Nullable String error) {
        if (error == null) return null;
        return error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR);
    }
}
