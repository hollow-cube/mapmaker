package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.util.Format;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SchemListCommand extends CommandDsl {

    public SchemListCommand() {
        super("list");

        addSyntax(playerOnly(this::handleListSchematics));
    }

    private void handleListSchematics(@NotNull Player player, @NotNull CommandContext context) {
        try {
            var session = PlayerSession.forPlayer(player);
            var schematics = session.terraform().storage().listSchematics(session.id());
            schematics.forEach(s -> player.sendMessage(s.name() + " " + s.dimensions() + " " + Format.formatBytes(s.size())));
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }
}
