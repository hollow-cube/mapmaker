package net.hollowcube.terraform.command.lib.mock;

import net.hollowcube.terraform.command.lib.Argument;
import net.hollowcube.terraform.command.lib.Command;
import net.hollowcube.terraform.session.Clipboard;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlipCommand extends Command {

    private final Argument<Integer> axisArg = null;
    private final Argument<Clipboard> clipboardArg = null;

    public FlipCommand() {
        super("flip");

        addSyntax(this::execute, axisArg, clipboardArg);
        addSyntax(this::execute, clipboardArg);
    }

    private void execute(@NotNull Player player, @NotNull Context context) {

    }

}
