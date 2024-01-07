package net.hollowcube.terraform.command.tool;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.tool.ToolHandler;
import org.jetbrains.annotations.NotNull;

public class ToolCommand extends CommandDsl {
    public ToolCommand(@NotNull ToolHandler toolHandler) {
        super("tool");

        addSubcommand(new ToolCreateCommand(toolHandler));
    }
}
