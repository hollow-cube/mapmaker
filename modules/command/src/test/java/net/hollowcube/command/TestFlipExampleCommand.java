package net.hollowcube.command;

import net.hollowcube.command.arg.ArgumentAxis;
import net.hollowcube.command.example.FlipCommand;
import net.hollowcube.command.util.FakePlayer;
import net.hollowcube.command.util.MockExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestFlipExampleCommand {
    private final CommandManager manager = new CommandManager();
    private final MockExecutor executor = new MockExecutor();
    private final MockExecutor errorHandler = new MockExecutor();
    private final FlipCommand flip = new FlipCommand(executor, errorHandler);

    @BeforeEach
    void setup() {
        manager.register(flip);
    }

    @Nested
    class Suggestions {

        @Test
        void emptyIsValid() {
            // Should be valid because the two args are both optional.
            var suggestion = manager.suggestions(new FakePlayer(), "flip");
            assertEquals(0, suggestion.getEntries().size(), "expected 0, got: " + suggestion.getEntries());
        }

        @Test
        void axisOnlySyntax() {
            var suggestion = manager.suggestions(new FakePlayer(), "flip ");
            assertEquals(3, suggestion.getEntries().size(), "expected 3, got: " + suggestion.getEntries()); // x | y | z
        }

        @Test
        void axisClipboardSyntax() {
            var suggestion = manager.suggestions(new FakePlayer(), "flip x clip");
            assertEquals(2, suggestion.getEntries().size(), "expected 2, got: " + suggestion.getEntries());
        }

        @Test
        void clipboardOnlySyntax() {
            var suggestion = manager.suggestions(new FakePlayer(), "flip c");
            assertEquals(2, suggestion.getEntries().size(), "expected 2, got: " + suggestion.getEntries());
        }

    }

    @Nested
    class Execution {

        @Test
        void emptyIsValid() {
            // Should be valid because the two args are both optional.
            manager.execute(new FakePlayer(), "flip");
            var context = executor.assertCalled();
            assertNull(context.get(flip.optAxisArg));
            assertNull(context.get(flip.optWordArg));
        }

        @Test
        void axisOnlyValid() {
            // Should be valid because the two args are both optional.
            manager.execute(new FakePlayer(), "flip x");
            var context = executor.assertCalled();
            var axis = assertInstanceOf(ArgumentAxis.Result.class, context.get(flip.optAxisArg));
            assertTrue(axis.x());
            assertNull(context.get(flip.optWordArg));
        }

        @Test
        void wordOnlyValid() {
            // Should be valid because the two args are both optional.
            manager.execute(new FakePlayer(), "flip clip1");
            var context = executor.assertCalled();
            assertNull(context.get(flip.optAxisArg));
            var word = assertInstanceOf(String.class, context.get(flip.optWordArg));
            assertEquals("clip1", word);
        }

        @Test
        void axisAndWordValid() {
            // Should be valid because the two args are both optional.
            manager.execute(new FakePlayer(), "flip xy clip_2");
            var context = executor.assertCalled();
            var axis = assertInstanceOf(ArgumentAxis.Result.class, context.get(flip.optAxisArg));
            assertTrue(axis.x());
            var word = assertInstanceOf(String.class, context.get(flip.optWordArg));
            assertEquals("clip_2", word);
        }

        @Test
        void testWordErrorHandler() {
            manager.execute(new FakePlayer(), "flip not_a_clipboard");
            var context = errorHandler.assertCalled();
        }

    }

}
