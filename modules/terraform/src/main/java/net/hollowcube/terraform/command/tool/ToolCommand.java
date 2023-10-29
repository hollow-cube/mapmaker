package net.hollowcube.terraform.command.tool;

import net.hollowcube.command.Command;
import net.hollowcube.terraform.tool.ToolHandler;
import org.jetbrains.annotations.NotNull;

public class ToolCommand extends Command {
    public ToolCommand(@NotNull ToolHandler toolHandler) {
        super("tool");

        addSubcommand(new ToolCreateCommand(toolHandler));
    }
}
