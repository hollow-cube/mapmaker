package net.hollowcube.command;

import org.junit.jupiter.api.Test;

class TestConditions extends BaseCommandTest {

    @Test
    void allowSuggest() {
        manager.register("test", test -> test
                .condition(condAllow)
                .child("a", a -> {
                }));

        assertSuggestions("test ", "a");
    }

    @Test
    void allowExecute() {
        manager.register("test", test -> test
                .condition(condAllow)
                .executes(mockExecutor()));

        assertSuccess("test");
    }

    @Test
    void denySuggest() {
        manager.register("test", test -> test
                .condition(condDeny)
                .child("a", a -> {
                }));

        assertSuggestions("test ", "a");
    }

    @Test
    void denyExecute() {
        manager.register("test", test -> test
                .condition(condDeny)
                .executes(mockExecutor()));

        assertDenied("test");
    }

    @Test
    void hideSuggest() {
        manager.register("test", test -> test
                .condition(condHide)
                .child("a", a -> {
                }));

        assertSuggestions("test ");
    }

    @Test
    void hideExecute() {
        manager.register("test", test -> test
                .condition(condHide)
                .executes(mockExecutor()));

        assertSyntaxError("test");
    }

    @Test
    void subTreeSuggestRegression() {
        manager.register("map", map -> map
                .child("list", list -> list.executes(mockExecutor()))
                .child("alter", alter -> alter
                        .condition(condHide)
                        .child("name", name -> name.executes(mockExecutor()))
                ));

        assertSuggestions("map ", "list");
    }
}
