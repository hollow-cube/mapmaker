package net.hollowcube.terraform.command;

import net.hollowcube.terraform.command.argument.ExtraArguments;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.util.schem.Rotation;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClipboardCommands {
    private ClipboardCommands() {}

    public static final class Copy extends Command {
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Copy(@Nullable CommandCondition condition) {
            super("copy", "tf:copy");
            setCondition(condition);

            setDefaultExecutor(this::handleCopySelection);
            addSyntax(this::handleCopySelection, selectionArg);
        }

        private void handleCopySelection(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            // Determine the target selection
            var session = LocalSession.forPlayer(player);
            Selection selection;
            if (context.has(selectionArg)) {
                selection = session.selection(context.get(selectionArg));
            } else {
                selection = session.selection(Selection.DEFAULT);
            }

            var region = selection.region();
            if (region == null) {
                sender.sendMessage(Component.translatable("command.terraform.no_selection"));
                return;
            }

            var playerSession = PlayerSession.forPlayer(player);
            session.action()
                    .at(player.getPosition())
                    .from(region)
                    .toSchematic(schem -> {
                        playerSession.setClipboard(schem);
                        sender.sendMessage(Component.translatable("command.terraform.copy.success"));
                    });
        }
    }

    public static final class Cut extends Command {
        private final Argument<String> selectionArg = ExtraArguments.Selection("selection");

        public Cut(@Nullable CommandCondition condition) {
            super("cut", "tf:cut");
            setCondition(condition);

            setDefaultExecutor(this::handleCutSelection);
            addSyntax(this::handleCutSelection, selectionArg);
        }

        private void handleCutSelection(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            // Determine the target selection
            var session = LocalSession.forPlayer(player);
            Selection selection;
            if (context.has(selectionArg)) {
                selection = session.selection(context.get(selectionArg));
            } else {
                selection = session.selection(Selection.DEFAULT);
            }

            sender.sendMessage("Not implemented - sorry :)");
            //todo
        }
    }

    public static final class Paste extends Command {

        public Paste(@Nullable CommandCondition condition) {
            super("paste", "tf:paste");
            setCondition(condition);

            setDefaultExecutor(this::handlePasteClipboard);
        }

        private void handlePasteClipboard(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("command.terraform.only_players"));
                return;
            }

            var playerSession = PlayerSession.forPlayer(player);
            var schem = playerSession.clipboard();
            if (schem == null) {
                player.sendMessage("Blah blah no clipboard");
                return;
            }

            schem.build(Rotation.NONE, null).apply(player.getInstance(), player.getPosition(), () -> {
                player.sendMessage("Done!");
            });

            //todo rewrite to use actions and add to history stack
        }
    }

}
