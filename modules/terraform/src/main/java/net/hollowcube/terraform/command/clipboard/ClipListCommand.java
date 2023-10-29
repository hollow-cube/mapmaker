package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.terraform.session.PlayerSession;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClipListCommand extends Command {

    public ClipListCommand() {
        super("list");

        addSyntax(playerOnly(this::handleListClipboards));
    }

    private void handleListClipboards(@NotNull Player player, @NotNull CommandContext context) {
        var session = PlayerSession.forPlayer(player);
        var clipboards = session.clipboardNames();

        if (clipboards.isEmpty()) {
            player.sendMessage("no clipboards");
            return;
        }

        for (var clipboard : session.clipboardNames()) {
            player.sendMessage(clipboard);
        }
    }

}
