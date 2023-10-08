package net.hollowcube.command;

import net.hollowcube.command.arg.ArgumentAxis;
import net.hollowcube.command.arg.SuggestionResult;
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
            var result = manager.suggestions(new FakePlayer(), "flip");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(0, success.suggestions().size(), "expected 0, got: " + success.suggestions());
        }

        @Test
        void axisOnlySyntax() {
            var result = manager.suggestions(new FakePlayer(), "flip ");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(3, success.suggestions().size(), "expected 3, got: " + success.suggestions()); // x | y | z
        }

        @Test
        void axisClipboardSyntax() {
            var result = manager.suggestions(new FakePlayer(), "flip x clip");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(2, success.suggestions().size(), "expected 2, got: " + success.suggestions()); // x | y | z
        }

        @Test
        void clipboardOnlySyntax() {
            var result = manager.suggestions(new FakePlayer(), "flip c");
            var success = assertInstanceOf(SuggestionResult.Success.class, result);
            assertEquals(2, success.suggestions().size(), "expected 2, got: " + success.suggestions()); // x | y | z
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
