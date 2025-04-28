package net.hollowcube.terraform.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.terraform.TerraformCancelCommand;
import net.hollowcube.terraform.command.terraform.TerraformDebugCommand;
import net.hollowcube.terraform.command.terraform.TerraformQueueCommand;
import net.hollowcube.terraform.command.terraform.TerraformToggleCuiCommand;
import net.hollowcube.terraform.util.Messages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TerraformCommand extends CommandDsl {

    public TerraformCommand() {
        super("terraform", "tf");

        description = "Manage terraform settings and debug options";

        addSyntax(playerOnly(this::showVersion), Argument.Literal("version"));

        addSubcommand(new TerraformDebugCommand());
        addSubcommand(new TerraformQueueCommand());
        addSubcommand(new TerraformCancelCommand());
        addSubcommand(new TerraformToggleCuiCommand());
    }

    private void showVersion(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage(Messages.TF_VERSION.with("1.0.0"));
    }
}
