package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.PlayerSession;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SchemLoadCommand extends Command {
    private final Argument<String> nameArg = Argument.Word("name");
    private final Argument<Clipboard> clipboardArg = TFArgument.Clipboard("clipboard");

    public SchemLoadCommand() {
        super("load");

        addSyntax(playerOnly(this::handleLoadSchematic), nameArg, clipboardArg);
    }

    private void handleLoadSchematic(@NotNull Player player, @NotNull CommandContext context) {
        var clipboard = context.get(clipboardArg);
        var name = context.get(nameArg);

        var session = PlayerSession.forPlayer(player);
        var storage = session.terraform().storage();
        var schemData = storage.loadSchematicData(session.id(), name);
        if (schemData == null) {
            player.sendMessage("Schematic not found: " + name);
            return;
        }

        clipboard.setData(schemData);
        player.sendMessage("loaded schematic to clipboard");
    }
}
