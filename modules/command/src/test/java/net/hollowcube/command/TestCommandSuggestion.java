package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
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
        var suggestion = manager.suggestions(new FakePlayer(), "notacommand");
        assertTrue(suggestion.isEmpty());
    }

    @Test
    void emptyCommandNoSyntax() {
        var manager = new CommandManager();
        manager.register(new EmptyCommand());
        var suggestion = manager.suggestions(new FakePlayer(), "empty");

        // This fails because there is no empty syntax at all
        assertTrue(suggestion.isEmpty());
    }

    @Test
    void emptyCommandEmptySyntax() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty);
        manager.register(cmd);

        var suggestion = manager.suggestions(new FakePlayer(), "empty");
        assertTrue(suggestion.isEmpty());
    }

    @Test
    void emptyCommandDefaultExecutor() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.setDefaultExecutor(empty);
        manager.register(cmd);

        var suggestion = manager.suggestions(new FakePlayer(), "empty");
        assertTrue(suggestion.isEmpty());
    }

    @Test
    void topLevelSingleSyntax() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var suggestion = manager.suggestions(new FakePlayer(), "empty tr");
        assertEquals(6, suggestion.getStart());
        assertEquals(2, suggestion.getLength());
        assertEquals(1, suggestion.getEntries().size());
        var first = suggestion.getEntries().get(0);
        assertEquals("true", first.replacement());
        assertNull(first.tooltip());
    }

    @Test
    void topLevelSingleSyntaxNoMatch() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var suggestion = manager.suggestions(new FakePlayer(), "empty a");
        assertTrue(suggestion.isEmpty());
    }

    @Test
    void topLevelSingleSyntaxMatchWithExtra() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var suggestion = manager.suggestions(new FakePlayer(), "empty true extratext");
        assertTrue(suggestion.isEmpty());
    }

    @Test
    void topLevelTwoLevelSyntax() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b1"), Argument.Bool("b2"));
        manager.register(cmd);

        var suggestion = manager.suggestions(new FakePlayer(), "empty true fal");
        assertEquals(11, suggestion.getStart());
        assertEquals(3, suggestion.getLength());
        assertEquals(1, suggestion.getEntries().size());
        var first = suggestion.getEntries().get(0);
        assertEquals("false", first.replacement());
    }

    @Test
    void topLevelMultiSyntaxSecondMatch() {
        var manager = new CommandManager();
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        cmd.addSyntax(empty, Argument.Word("w").with("foo", "bar", "baz"));
        manager.register(cmd);

        var suggestion = manager.suggestions(new FakePlayer(), "empty fo");
        assertEquals(6, suggestion.getStart());
        assertEquals(2, suggestion.getLength());
        assertEquals(1, suggestion.getEntries().size());
        var first = suggestion.getEntries().get(0);
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
        var suggestion = manager.suggestions(new FakePlayer(), "empty tr");
        assertTrue(suggestion.isEmpty());
    }

}
