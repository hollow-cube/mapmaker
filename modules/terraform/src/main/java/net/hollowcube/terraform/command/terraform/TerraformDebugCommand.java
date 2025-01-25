package net.hollowcube.terraform.command.terraform;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class TerraformDebugCommand extends CommandDsl {
    public TerraformDebugCommand() {
        super("debug");

        addSyntax(playerOnly(this::showSessionDebug), Argument.Literal("session"));
        addSyntax(playerOnly(this::showThreadsDebug), Argument.Literal("threads"));
    }

    private void showSessionDebug(@NotNull Player player, @NotNull CommandContext context) {
        // Print player session info
        var session = PlayerSession.forPlayer(player);
        player.sendMessage(Component.text("Session:"));
        player.sendMessage(Component.text("  Clipboards: " + String.join(",", session.clipboardNames())));

        // Print local session info
        var localSession = LocalSession.forPlayer(player);
        player.sendMessage(Component.text("Local Session:"));
        player.sendMessage(Component.text("  Instance: " + localSession.instance().getUuid()));
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

    private void showThreadsDebug(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("todo need to do fancy stuff and i dont want to right now");
    }

}
