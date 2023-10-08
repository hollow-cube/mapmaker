package net.hollowcube.command;

import net.hollowcube.command.arg.SuggestionResult;
import net.hollowcube.command.example.ListCommand;
import net.hollowcube.command.util.FakePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TestListExampleCommand {
    private final CommandManager manager = new CommandManager();

    @BeforeEach
    void setup() {
        manager.register(new ListCommand());
    }

    @Nested
    class Suggestions {

        @Test
        void emptyIsValidForPlayer() {
            var result = manager.suggestions(new FakePlayer(), "list");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(0, success.suggestions().size(), "expected 0, got: " + success.suggestions());
        }

        @Test
        void emptyIsValidForConsole() {
            var result = manager.suggestions(FakePlayer.CONSOLE, "list");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(0, success.suggestions().size(), "expected 0, got: " + success.suggestions());
        }

        @Test
        void completeTargetForConsole() {
            var result = manager.suggestions(FakePlayer.CONSOLE, "list n");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(1, success.suggestions().size(), "expected 1, got: " + success.suggestions());
        }

        @Test
        void defaultExecutorForTargetForPlayer() {
            var result = manager.suggestions(new FakePlayer(), "list n");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(0, success.suggestions().size(), "expected 0, got: " + success.suggestions());
        }

    }

}
