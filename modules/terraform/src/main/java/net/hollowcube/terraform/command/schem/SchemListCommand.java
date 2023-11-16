package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.util.Format;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SchemListCommand extends Command {

    public SchemListCommand() {
        super("list");

        addSyntax(playerOnly(this::handleListSchematics));
    }

    private void handleListSchematics(@NotNull Player player, @NotNull CommandContext context) {
        var session = PlayerSession.forPlayer(player);
        var schematics = session.terraform().storage().listSchematics(session.id());
        schematics.forEach(s -> player.sendMessage(s.name() + " " + s.dimensions() + " " + Format.formatBytes(s.size())));
    }
}
