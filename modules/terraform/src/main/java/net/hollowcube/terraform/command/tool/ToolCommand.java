package net.hollowcube.terraform.command.tool;

import com.google.inject.Inject;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.Terraform;
import org.jetbrains.annotations.NotNull;

public class ToolCommand extends CommandDsl {

    @Inject
    public ToolCommand(@NotNull Terraform tf) {
        super("tool");

        addSubcommand(new ToolCreateCommand(tf.toolHandler()));
    }
}
