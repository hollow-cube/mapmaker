package net.hollowcube.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandExecutor;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MockExecutor implements CommandExecutor {
    private CommandContext context = null;

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {
        this.context = context;
    }

    public @NotNull CommandContext assertCalled() {
        assertNotNull(context, "executor not called");
        return context;
    }
}
