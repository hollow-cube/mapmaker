package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.example.EmptyCommand;
import net.hollowcube.command.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestCommandExpansion {

    private static final CommandExecutor empty = (sender, context) -> {
    };
    private static final CommandCondition hide = (sender, context) -> CommandCondition.HIDE;


    private final CommandManager manager = new CommandManager();

    @Test
    void noCommand() {
        var context = expand("cmd");
        assertTrue(context.isFailed());
    }

    @Test
    void noCommandMatchingCondition() {
        var cmd = new EmptyCommand();
        cmd.setCondition(hide);
        manager.register(cmd);
        var context = expand("cmd");
        assertTrue(context.isFailed());
    }

    @Test
    void empty() {
        var cmd = new EmptyCommand();
        manager.register(cmd);
        var context = expand("empty");
        assertSame(cmd, context.command());
    }

    @Test
    void topLevelSingleSyntax() {
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var context = expand("empty tr");
        assertSame(cmd, context.command());
        assertFalse(context.isFailed());
        assertEquals(1, context.argValues().size());
        assertNull(context.argValues().get(0));
    }

    @Test
    void topLevelSingleSyntaxNoMatch() {
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var context = expand("empty a");
        assertTrue(context.isFailed());
    }

    @Test
    void topLevelSingleSyntaxMatchWithExtra() {
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        manager.register(cmd);

        var context = expand("empty true extratext");
        assertTrue(context.isFailed());
    }

    @Test
    void topLevelTwoLevelSyntax() {
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b1"), Argument.Bool("b2"));
        manager.register(cmd);

        var result = expand("empty true fal");
        assertFalse(result.isFailed());
        assertEquals(2, result.argValues().size());
        assertTrue(result.argValues().get(0) instanceof Boolean b && b);
        assertNull(result.argValues().get(1));
    }

    @Test
    void topLevelMultiSyntaxSecondMatch() {
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Bool("b"));
        cmd.addSyntax(empty, Argument.Word("w").with("foo", "bar", "baz"));
        manager.register(cmd);

        var context = expand("empty fo");
        assertFalse(context.isFailed());
        assertEquals(1, context.argValues().size());
        assertNull(context.argValues().get(0));
    }

    @Test
    void skippedSyntaxDoesNotDefaultToEmpty() {
        var cmd = new EmptyCommand();
        cmd.addSyntax(hide, empty, Argument.Bool("b")); // Condition syntax which is hidden
        cmd.addSyntax(empty); // Empty syntax
        manager.register(cmd);

        // Normally this would match the bool syntax, but it is hidden, so it should fail.
        // It cannot go to the empty syntax because there are trailing arguments.
        var context = expand("empty tr");
        assertTrue(context.isFailed());
    }

    @Test
    void optionalArgCanBeIgnored() {
        var cmd = new EmptyCommand();
        cmd.addSyntax(empty, Argument.Opt(Argument.Bool("b")));
        manager.register(cmd);

        var context = expand("empty");
        assertFalse(context.isFailed());
    }

    private CommandContextImpl expand(@NotNull String input) {
        return manager.expand(CommandContext.Pass.SUGGEST, new FakePlayer(), input);
    }
}
