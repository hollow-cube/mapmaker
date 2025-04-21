package net.hollowcube.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

class TestSuggestions extends BaseCommandTest {

    @Test
    void leafMatch() {
        assumeFalse(true, "TODO: fix this");
        manager.register("test", new CommandNode());

        assertSuggestions("test");
    }

    @Test
    void leafInvalidExtra() {
        manager.register("test", new CommandNode());

        assertSuggestions("test abc");
    }

    @Test
    void exactMatchNoSpace() {
        assumeFalse(true, "TODO: fix this");
        manager.register("test", new CommandBuilder()
                .child("a", builder -> {
                })
                .node());

        assertSuggestions("test", "");
    }

    @Test
    void singleChildExactMatchTrailingSpace() {
        manager.register("test", new CommandBuilder()
                .child("a", builder -> {
                })
                .node());

        assertSuggestions("test ", "a");
    }

    @Test
    void multiChildExactMatchTrailingSpace() {
        manager.register("test", new CommandBuilder()
                .child("a", builder -> {
                })
                .child("b", builder -> {
                })
                .node());

        assertSuggestions("test ", "a", "b");
    }


}
