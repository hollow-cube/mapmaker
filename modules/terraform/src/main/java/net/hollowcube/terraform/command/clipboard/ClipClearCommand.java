package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.command.util.TFArgument;
import net.hollowcube.terraform.session.Clipboard;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClipClearCommand extends CommandDsl {
    private final Argument<Clipboard> clipboardArg = TFArgument.Clipboard("clipboard");

    public ClipClearCommand() {
        super("clear");

        addSyntax(playerOnly(this::handleClearClipboard), clipboardArg);
    }

    private void handleClearClipboard(@NotNull Player player, @NotNull CommandContext context) {
        var clipboard = context.get(clipboardArg);
        clipboard.clear();

        player.sendMessage(Component.translatable("terraform.clipboard.clear"));
    }

}
