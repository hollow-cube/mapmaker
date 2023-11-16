package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SchemDownloadCommand extends Command {

    public SchemDownloadCommand() {
        super("download");

        setDefaultExecutor(this::handleDownloadSchematic);
    }

    private void handleDownloadSchematic(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage("downloads are handled via discord, use /schem download in any channel or a DM with Hollow Cube Bot (the download will not be public)");
    }
}
