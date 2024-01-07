package net.hollowcube.terraform.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.util.DebugCommand;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TerraformCommand extends CommandDsl {

    public TerraformCommand() {
        super("terraform");

        addSyntax(playerOnly(this::showVersion), Argument2.Literal("version"));

        addSubcommand(new DebugCommand());
    }

    private void showVersion(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("Terraform vTODO");
    }
}
