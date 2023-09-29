package net.hollowcube.terraform.command.lib.mock;

import net.hollowcube.terraform.command.lib.Argument;
import net.hollowcube.terraform.command.lib.Command;
import net.hollowcube.terraform.session.Clipboard;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlipCommand extends Command {

    /*

    3 states: not applicable, applicable w/ error, matched
    syntax order determines which is matched first, so for example to do something like `/c flip [axis] [clipboard]` where you can do
    - /c flip my_clipboard
    - /c flip x my_clipboard

    there would be the following syntaxes here:
    - /c flip axis clipboard
    - /c flip axis
    - /c flip clipboard
    - /c flip
    which could be condensed with optionals
    - /c flip axis opt[clipboard]
    - /c flip opt[clipboard]
    first we try to match an axis as the first arg and send suggestions for that one (eg x, xy, xyz)
    then if it cannot match (not one of the above), we ignore that syntax and try to match the second one and send suggestions for it.

    Subcommands match first, then syntaxes, then arguments.
     */

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
