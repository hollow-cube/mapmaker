package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SchemDownloadCommand extends CommandDsl {

    public SchemDownloadCommand() {
        super("download");

        addSyntax(this::handleDownloadSchematic);
    }

    private void handleDownloadSchematic(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage("downloads are handled via discord, use /schem download in any channel or a DM with Hollow Cube Bot (the download will not be public)");
    }
}
