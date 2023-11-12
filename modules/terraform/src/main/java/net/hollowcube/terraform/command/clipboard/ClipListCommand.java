package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
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
            player.sendMessage("terraform.clipboard.list.none");
            return;
        }

        for (var clipboard : session.clipboardNames()) {
            player.sendMessage(Component.translatable("terraform.clipboard.list", Component.translatable(clipboard)));
        }
    }

}
