package net.hollowcube.command;

import net.hollowcube.command.example.ListCommand;
import net.hollowcube.command.util.FakePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            var suggestion = manager.suggestions(new FakePlayer(), "list");
            assertEquals(0, suggestion.getEntries().size(), "expected 0, got: " + suggestion.getEntries());
        }

        @Test
        void emptyIsValidForConsole() {
            var suggestion = manager.suggestions(FakePlayer.CONSOLE, "list");
            assertEquals(0, suggestion.getEntries().size(), "expected 0, got: " + suggestion.getEntries());
        }

        @Test
        void completeTargetForConsole() {
            var suggestion = manager.suggestions(FakePlayer.CONSOLE, "list n");
            assertEquals(1, suggestion.getEntries().size(), "expected 1, got: " + suggestion.getEntries());
        }

        @Test
        void defaultExecutorForTargetForPlayer() {
            var suggestion = manager.suggestions(new FakePlayer(), "list n");
            assertEquals(0, suggestion.getEntries().size(), "expected 0, got: " + suggestion.getEntries());
        }

    }

}
