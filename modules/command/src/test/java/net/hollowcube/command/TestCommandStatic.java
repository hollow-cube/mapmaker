package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.util.TestCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCommandStatic {
    private static final CommandExecutor exec = (sender, context) -> {
    };
    private static final CommandCondition cond = (sender, context) -> CommandCondition.HIDE;

    @Test
    void emptyIsStatic() {
        var command = new TestCommand("empty");
        assertTrue(command.isStatic());
    }

    @Test
    void singleNonConditionalSyntaxIsStatic() {
        var command = new TestCommand("empty");
        command.addSyntax(exec, Argument.Word("test"));
        assertTrue(command.isStatic());
    }

    @Test
    void singleConditionalSyntaxIsNotStatic() {
        var command = new TestCommand("empty");
        command.addSyntax(cond, exec, Argument.Word("test"));
        assertFalse(command.isStatic());
    }
}
