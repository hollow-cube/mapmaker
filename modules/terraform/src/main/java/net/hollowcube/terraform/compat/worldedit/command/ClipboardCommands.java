package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.compat.worldedit.util.CommandUtil;
import net.hollowcube.terraform.session.Session;
import net.hollowcube.util.schem.Rotation;
import net.hollowcube.util.schem.SchematicBuilder;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClipboardCommands {
    public ClipboardCommands(@NotNull CommandManager commands) {
        commands.register(CommandUtil.singleSyntaxCommand("/copy", this::copy));
        commands.register(CommandUtil.singleSyntaxCommand("/paste", this::paste));
        commands.register(new RotateCommand());
    }

    public void copy(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var session = Session.forPlayer(player);
        var selection = session.getRegionSelector(player.getInstance()).getRegion();
        if (selection == null) {
            player.sendMessage("No region selected");
            return;
        }

        var builder = new SchematicBuilder();
        builder.setOffset(selection.min().sub(player.getPosition()));
        for (var pos : selection) {
            var block = selection.instance().getBlock(pos);
            builder.addBlock(pos, block);
        }

        session.getClipboard().set(builder.toSchematic());
        sender.sendMessage("Copied selection to clipboard");
    }

    public void paste(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var clipboard = Session.forPlayer(player).getClipboard();
        var schem = clipboard.get();
        if (schem == null) {
            player.sendMessage("No clipboard contents");
            return;
        }

        //todo need to add to history. Should make a generic api for applying
        schem.build(clipboard.getCurrentRotation(), null).apply(player.getInstance(), player.getPosition(), null);
        sender.sendMessage("Pasted clipboard contents");
    }

    public static class RotateCommand extends Command {
        private final ArgumentWord rotateArg = ArgumentType.Word("rotation").from("90", "180", "270");

        public RotateCommand() {
            super("/rotate");

            setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: //rotate <degrees>"));
            addSyntax(this::rotateClipboard, rotateArg);
        }
        public void rotateClipboard(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player))
                throw new UnsupportedOperationException("only implemented for players");

            var clipboard = Session.forPlayer(player).getClipboard();

            if (clipboard.get() == null) {
                player.sendMessage("Nothing in your clipboard to rotate.");
                return;
            }

            var degrees = context.get(rotateArg);
            switch (degrees) {
                case "90" -> clipboard.setCurrentRotation(Rotation.CLOCKWISE_90);
                case "180" -> clipboard.setCurrentRotation(Rotation.CLOCKWISE_180);
                case "270" -> clipboard.setCurrentRotation(Rotation.CLOCKWISE_270);
                default -> player.sendMessage("Unknown rotation value " + degrees);
            }
        }
    }
}
