package net.hollowcube.command;


import org.junit.jupiter.api.Test;

class TestValidExecution extends BaseCommandTest {

    @Test
    void rootSyntaxNoChildren() {
        var executor = mockExecutor();
        manager.register("test", new CommandBuilder()
                .executes(executor)
                .node());

        manager.execute(sender, "test");
        assertExecuted(executor);
    }

    @Test
    void singleChild() {
        var executor = mockExecutor();
        manager.register("test", new CommandBuilder()
                .child("a", a -> a.executes(executor))
                .node());

        manager.execute(sender, "test a");
        assertExecuted(executor);
    }

    @Test
    void twoLevelsChildren() {
        var executor = mockExecutor();
        manager.register("test", new CommandBuilder()
                .child("a", a -> a
                        .child("b", b -> b.executes(executor)))
                .node());

        manager.execute(sender, "test a b");
        assertExecuted(executor);
    }

    @Test
    void twoChildren() {
        var executor1 = mockExecutor();
        var executor2 = mockExecutor();
        manager.register("test", new CommandBuilder()
                .child("a", a -> a.executes(executor1))
                .child("b", b -> b.executes(executor2))
                .node());

        manager.execute(sender, "test a");
        assertExecuted(executor1);
        assertNotExecuted(executor2);

        manager.execute(sender, "test b");
        assertNotExecuted(executor1);
        assertExecuted(executor2);
    }

}
