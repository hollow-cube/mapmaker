package net.hollowcube.command;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.suggestion.SuggestionEntry;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class BaseCommandTest {
    protected final CommandManagerImpl manager = new CommandManagerImpl();
    protected final CommandSender sender = new ConsoleSender();

    protected final CommandCondition condAllow = (sender, context) -> CommandCondition.ALLOW;
    protected final CommandCondition condDeny = (sender, context) -> CommandCondition.DENY;
    protected final CommandCondition condHide = (sender, context) -> CommandCondition.HIDE;

    protected @NotNull CommandResult.Success assertSuccess(@NotNull String input) {
        var result = manager.execute(sender, input);
        return assertInstanceOf(CommandResult.Success.class, result);
    }

    protected @NotNull CommandResult.SyntaxError assertSyntaxError(@NotNull String input) {
        var result = manager.execute(sender, input);
        return assertInstanceOf(CommandResult.SyntaxError.class, result);
    }

    protected @NotNull CommandResult.Denied assertDenied(@NotNull String input) {
        var result = manager.execute(sender, input);
        return assertInstanceOf(CommandResult.Denied.class, result);
    }

    protected void assertSuggestions(@NotNull String input, @NotNull String... suggestions) {
        assertSuggestions(manager.suggest(sender, input), suggestions);
    }

    protected void assertSuggestions(@NotNull Suggestion suggestion, @NotNull String... suggestions) {
        if (suggestions.length == 0) {
            assertEquals(0, suggestion.getEntries().size(), "Expected no suggestions");
        } else {
            assertEquals(
                    //todo stringifying these is really yikes
                    Stream.of(suggestions).sorted().toList().toString(),
                    suggestion.getEntries().stream().map(SuggestionEntry::replacement).sorted().toList().toString()
            );
        }
    }

    protected void assertExecuted(@NotNull MockCommandExecutor mock) {
        assertTrue(mock.executed, "Command executor was not executed");
        mock.executed = false;
    }

    protected void assertNotExecuted(@NotNull MockCommandExecutor mock) {
        assertFalse(mock.executed, "Command executor was executed");
        mock.executed = false;
    }

    protected @NotNull MockCommandExecutor mockExecutor() {
        return new MockCommandExecutor();
    }

    public static class MockCommandExecutor implements CommandExecutor {
        private boolean executed = false;

        @Override
        public void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {
            this.executed = true;
        }
    }
}
