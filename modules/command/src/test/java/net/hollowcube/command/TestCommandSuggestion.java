package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.SuggestionResult;
import net.hollowcube.command.example.EmptyCommand;
import net.hollowcube.command.util.FakePlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestCommandSuggestion {
    private static final CommandExecutor empty = (sender, context) -> {
    };
    private static final CommandCondition hide = (sender, context) -> CommandCondition.HIDE;

    @Test
    void missingCommand() {
        var manager = new CommandManager();
        var result = manager.suggestions(new FakePlayer(), "notacommand");
        assertInstanceOf(SuggestionResult.Failure.class, result);
    }

    @Test
    void emptyCommandNoSyntax() {
        var manager = new CommandManager();
        manager.register(new EmptyCommand());
        var result = manager.suggestions(new FakePlayer(), "empty");

        // This fails because there is no empty syntax at all
        assertInstanceOf(SuggestionResult.Failure.class, result);
    }

    @Test
    void emptyCommandEmptySyntax() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty);
        manager.register(cmd);

        var result = manager.suggestions(new FakePlayer(), "empty");
        var success = assertInstanceOf(SuggestionResult.Success.class, result);
        assertEquals(0, success.suggestions().size());
    }

    @Test
    void emptyCommandDefaultExecutor() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.setDefaultExecutor(empty);
        manager.register(cmd);

        var result = manager.suggestions(new FakePlayer(), "empty");
        var success = assertInstanceOf(SuggestionResult.Success.class, result);
        assertEquals(0, success.suggestions().size());
    }

    @Test
    void topLevelSingleSyntax() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var result = manager.suggestions(new FakePlayer(), "empty tr");
        var success = assertInstanceOf(SuggestionResult.Success.class, result);
        assertEquals(6, success.start());
        assertEquals(2, success.length());
        assertEquals(1, success.suggestions().size());
        var first = success.suggestions().get(0);
        assertEquals("true", first.replacement());
        assertNull(first.tooltip());
    }

    @Test
    void topLevelSingleSyntaxNoMatch() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var result = manager.suggestions(new FakePlayer(), "empty a");
        assertInstanceOf(SuggestionResult.Failure.class, result);
    }

    @Test
    void topLevelSingleSyntaxMatchWithExtra() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var result = manager.suggestions(new FakePlayer(), "empty true extratext");
        assertInstanceOf(SuggestionResult.Failure.class, result);
    }

    @Test
    void topLevelTwoLevelSyntax() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b1"), Argument.Bool("b2"));
        manager.register(cmd);

        var result = manager.suggestions(new FakePlayer(), "empty true fal");
        var success = assertInstanceOf(SuggestionResult.Success.class, result);
        assertEquals(11, success.start());
        assertEquals(3, success.length());
        assertEquals(1, success.suggestions().size());
        var first = success.suggestions().get(0);
        assertEquals("false", first.replacement());
    }

    @Test
    void topLevelMultiSyntaxSecondMatch() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        cmd.addSyntax(empty, Argument.Word("w").with("foo", "bar", "baz"));
        manager.register(cmd);

        var result = manager.suggestions(new FakePlayer(), "empty fo");
        var success = assertInstanceOf(SuggestionResult.Success.class, result);
        assertEquals(6, success.start());
        assertEquals(2, success.length());
        assertEquals(1, success.suggestions().size());
        var first = success.suggestions().get(0);
        assertEquals("foo", first.replacement());
    }

    @Test
    void skippedSyntaxDoesNotDefaultToEmpty() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(hide, empty, Argument.Bool("b")); // Condition syntax which is hidden
        cmd.addSyntax(empty); // Empty syntax
        manager.register(cmd);

        // Normally this would match the bool syntax, but it is hidden, so it should fail.
        // It cannot go to the empty syntax because there are trailing arguments.
        var result = manager.suggestions(new FakePlayer(), "empty tr");
        assertInstanceOf(SuggestionResult.Failure.class, result);
    }

}
