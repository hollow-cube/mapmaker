package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SchemUploadCommand extends Command {

    public SchemUploadCommand() {
        super("upload");

        setDefaultExecutor(this::handleDownloadSchematic);
    }

    private void handleDownloadSchematic(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage("uploads are handled via discord, use /schem upload in any channel or a DM with Hollow Cube Bot (the upload will not be public)");
    }
}
