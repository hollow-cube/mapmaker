package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.PlayerSession;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SchemSaveCommand extends CommandDsl {
    private static final Argument<String> nameArg = Argument.Word("name");
    private static final Argument<Clipboard> clipboardArg = TFArgument.Clipboard("clipboard");

    public SchemSaveCommand() {
        super("save");

        addSyntax(playerOnly(this::handleSaveSchematic), nameArg);
        addSyntax(playerOnly(this::handleSaveSchematic), nameArg, clipboardArg);
    }

    private void handleSaveSchematic(@NotNull Player player, @NotNull CommandContext context) {
        var clipboard = context.get(clipboardArg);
        var name = context.get(nameArg);

        var session = PlayerSession.forPlayer(player);
        var storage = session.terraform().storage();
        var schemData = clipboard.getInitialSchematic();
        if (schemData == null) {
            player.sendMessage("No schematic data in clipboard");
            return;
        }

        var result = storage.createSchematic(session.id(), name, schemData, false);
        switch (result) {
            case SUCCESS -> player.sendMessage("Schematic saved: " + name);
            case DUPLICATE_ENTRY -> player.sendMessage("Schematic already exists: " + name);
            case ENTRY_LIMIT_EXCEEDED -> player.sendMessage("Schematic limit exceeded");
            case SIZE_LIMIT_EXCEEDED -> player.sendMessage("Schematic size limit exceeded");
        }
    }
}
