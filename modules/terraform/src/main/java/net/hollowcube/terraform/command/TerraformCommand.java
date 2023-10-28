package net.hollowcube.terraform.command;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.command.util.DebugCommand;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TerraformCommand extends Command {

    public TerraformCommand() {
        super("terraform");

        addSyntax(playerOnly(this::showVersion), Argument.Literal("version"));

        addSubcommand(new DebugCommand());
    }

    private void showVersion(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("Terraform vTODO");
    }
}
