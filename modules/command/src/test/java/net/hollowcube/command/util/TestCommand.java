package net.hollowcube.command.util;

import net.hollowcube.command.Command;
import org.jetbrains.annotations.NotNull;

// Test command just exposes an API for configuring the command programatically
public class TestCommand extends Command {
    public TestCommand(@NotNull String name) {
        super(name);
    }
}
