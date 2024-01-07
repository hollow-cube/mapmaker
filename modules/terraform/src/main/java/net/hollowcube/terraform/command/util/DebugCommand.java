package net.hollowcube.terraform.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class DebugCommand extends CommandDsl {
    public DebugCommand() {
        super("debug");

        addSyntax(this::showHelp);
        addSyntax(playerOnly(this::showSessionDebug), Argument2.Literal("session"));
    }

    private void showHelp(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage("Available debug options:");
        sender.sendMessage("- session");
    }

    private void showSessionDebug(@NotNull Player player, @NotNull CommandContext context) {
        // Print player session info
        var session = PlayerSession.forPlayer(player);
        player.sendMessage(Component.text("Session:"));
//        player.sendMessage(Component.text("  Clipboard: " + (session.clipboard() != null ? "yes" : "no")));

        // Print local session info
        var localSession = LocalSession.forPlayer(player);
        player.sendMessage(Component.text("Local Session:"));
        player.sendMessage(Component.text("  Instance: " + localSession.instance().getUniqueId()));
        player.sendMessage(Component.text("  History: " + localSession.undoCount() + "/" + localSession.redoCount()));
        player.sendMessage(Component.text("  Selections: " + localSession.selectionNames().size()));

        // Print selection info
        for (var selectionName : localSession.selectionNames()) {
            var selection = localSession.selection(selectionName);
            player.sendMessage(Component.text("  Selection: " + selectionName));
            player.sendMessage(Component.text("    Type: " + selection.type().name().toLowerCase(Locale.ROOT)));
            player.sendMessage(Component.text("    Region: " + (selection.region() != null ? "yes" : "no")));
        }
    }

}
