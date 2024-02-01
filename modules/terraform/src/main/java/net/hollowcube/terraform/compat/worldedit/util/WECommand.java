package net.hollowcube.terraform.compat.worldedit.util;

import net.hollowcube.command.dsl.CommandDsl;
import org.jetbrains.annotations.NotNull;

public class WECommand extends CommandDsl {
    public WECommand(@NotNull String name, @NotNull String... aliases) {
        super(name, aliases);
    }
}
