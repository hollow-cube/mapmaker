package net.hollowcube.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class TestBadExecution extends BaseCommandTest {

    @Test
    void singleChildUnexpectedEndOfInput() {
        manager.register("test", new CommandBuilder()
                .child("a", a -> {
                })
                .node());

        var err = assertSyntaxError("test a");
        assertEquals(6, err.start());
    }

    @Test
    void singleChildExtraInput() {
        assumeFalse(true, "TODO: fix this");
        manager.register("test", new CommandBuilder()
                .child("a", a -> {
                })
                .node());

        var err = assertSyntaxError("test a b");
        assertEquals(7, err.start());
    }

    @Test
    void singleChildPartialMatch() {
        assumeFalse(true, "TODO: fix this");
        manager.register("test", new CommandBuilder()
                .child("abc", abc -> {
                })
                .node());

        var err = assertSyntaxError("test ab");
        assertEquals(5, err.start());
    }

    @Test
    void singleChildInvalidArg() {
        manager.register("test", new CommandBuilder()
                .child("a", a -> {
                })
                .node());

        var err = assertSyntaxError("test b");
        assertEquals(5, err.start());
    }

    @Test
    void multiChildInvalidArg() {
        manager.register("test", new CommandBuilder()
                .child("a", a -> {
                })
                .child("b", b -> {
                })
                .node());

        var err = assertSyntaxError("test c");
        assertEquals(5, err.start());
        // Should be an error on the first argument
        assertEquals("a", err.arg().id());
    }

}
