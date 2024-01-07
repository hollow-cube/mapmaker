package net.hollowcube.command;

import org.junit.jupiter.api.Test;

class TestSuggestions extends BaseCommandTest {

    @Test
    void leafMatch() {
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
        manager.register("test", new CommandBuilder()
                .child("a", builder -> builder)
                .node());

        assertSuggestions("test", "");
    }

    @Test
    void singleChildExactMatchTrailingSpace() {
        manager.register("test", new CommandBuilder()
                .child("a", builder -> builder)
                .node());

        assertSuggestions("test ", "a");
    }

    @Test
    void multiChildExactMatchTrailingSpace() {
        manager.register("test", new CommandBuilder()
                .child("a", builder -> builder)
                .child("b", builder -> builder)
                .node());

        assertSuggestions("test ", "a", "b");
    }


}
