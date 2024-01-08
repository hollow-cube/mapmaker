package net.hollowcube.command;

import org.junit.jupiter.api.Test;

class TestRedirects extends BaseCommandTest {

    @Test
    public void simpleRootAlias() {
        var executor = mockExecutor();
        manager.register("root", root -> root.executes(executor));
        manager.register("alias", alias -> alias.redirect(manager.xpath("root")));

        assertSuccess("alias");
        assertExecuted(executor);
    }

    @Test
    public void execAliasWithArgument() {
        var executor = mockExecutor();
        manager.register("root", root -> root.child("arg", arg -> arg.executes(executor)));
        manager.register("alias", alias -> alias.redirect(manager.xpath("root")));

        assertSuccess("alias arg");
        assertExecuted(executor);
    }

    @Test
    public void suggestAliasWithArgument() {
        manager.register("root", root -> root.child("arg", arg -> {
        }));
        manager.register("alias", alias -> alias.redirect(manager.xpath("root")));

        assertSuggestions("alias ", "arg");
    }
}
