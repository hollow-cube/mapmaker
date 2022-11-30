package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.compat.worldedit.util.CommandUtil;
import net.hollowcube.terraform.session.Session;
import net.hollowcube.util.schem.Rotation;
import net.hollowcube.util.schem.SchematicBuilder;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClipboardCommands {
    public ClipboardCommands(@NotNull CommandManager commands) {

        commands.register(CommandUtil.singleSyntaxCommand("/copy", this::copy));
        commands.register(CommandUtil.singleSyntaxCommand("/paste", this::paste));

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
            var block = player.getInstance().getBlock(pos);
            builder.addBlock(pos, block);
        }

        session.getClipboard().set(builder.toSchematic());
        sender.sendMessage("Copied selection to clipboard");
    }

    public void paste(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player))
            throw new UnsupportedOperationException("only implemented for players");

        var session = Session.forPlayer(player);
        var clipboard = session.getClipboard().get();
        if (clipboard == null) {
            player.sendMessage("No clipboard contents");
            return;
        }

        //todo need to add to history. Should make a generic api for applying
        clipboard.build(Rotation.NONE, null).apply(player.getInstance(), player.getPosition(), null);
        sender.sendMessage("Pasted clipboard contents");
    }

}
