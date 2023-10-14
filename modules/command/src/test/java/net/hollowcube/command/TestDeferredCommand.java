package net.hollowcube.command;

import net.hollowcube.command.example.DeferredCommand;
import net.hollowcube.command.util.FakePlayer;
import net.hollowcube.command.util.MockExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDeferredCommand {
    private final CommandManager manager = new CommandManager();
    private final MockExecutor executor = new MockExecutor();
    private final DeferredCommand command = new DeferredCommand(executor);

    @BeforeEach
    void setup() {
        manager.register(command);
    }

    @Nested
    class Suggestions {

    }

    @Nested
    class Execution {

        @Test
        void test() {
            manager.execute(new FakePlayer(), "defer abc");
            var context = executor.assertCalled();
            assertEquals("ABC", context.get(command.optWordArg));
        }

    }

}
