package net.hollowcube.terraform.command.tool;

import net.hollowcube.command.dsl.CommandDsl;

public class ToolCommand extends CommandDsl {

    public ToolCommand() {
        super("tool");

//        addSubcommand(new ToolCreateCommand(tf.toolHandler()));
    }
}
