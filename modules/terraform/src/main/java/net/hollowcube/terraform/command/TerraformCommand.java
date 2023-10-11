package net.hollowcube.terraform.command;

import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class TerraformCommand extends Command {
    public TerraformCommand(@Nullable CommandCondition condition) {
        super("terraform", "tf:terraform");
        setCondition(condition);

        addSyntax(this::showTerraformInfo);

        addSubcommand(new Debug());
    }

    private void showTerraformInfo(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage("todo show terraform info");
    }

    public static final class Debug extends Command {
        public Debug() {
            super("debug");

            addSubcommand(new Session());
        }

        public static final class Session extends Command {

            public Session() {
                super("session");

                setDefaultExecutor(this::showSessionDebug);
            }

            private void showSessionDebug(@NotNull CommandSender sender, @NotNull CommandContext context) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.translatable("generic.players_only"));
                    return;
                }

                // Print player session info
                var session = PlayerSession.forPlayer(player);
                sender.sendMessage(Component.text("Session:"));
//                sender.sendMessage(Component.text("  Clipboard: " + (session.clipboard() != null ? "yes" : "no")));

                // Print local session info
                var localSession = LocalSession.forPlayer(player);
                sender.sendMessage(Component.text("Local Session:"));
                sender.sendMessage(Component.text("  Instance: " + localSession.instance().getUniqueId()));
                sender.sendMessage(Component.text("  History: " + localSession.undoCount() + "/" + localSession.redoCount()));
                sender.sendMessage(Component.text("  Selections: " + localSession.selectionNames().size()));

                // Print selection info
                for (var selectionName : localSession.selectionNames()) {
                    var selection = localSession.selection(selectionName);
                    sender.sendMessage(Component.text("  Selection: " + selectionName));
                    sender.sendMessage(Component.text("    Type: " + selection.type().name().toLowerCase(Locale.ROOT)));
                    sender.sendMessage(Component.text("    Region: " + (selection.region() != null ? "yes" : "no")));
                }
            }
        }
    }

}
