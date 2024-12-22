package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MinestomCommand extends CommandDsl {

    public MinestomCommand() {
        super("minestom", "plugins", "pl", "server");

        addSyntax(this::sendMinestomInfo);
    }

    private void sendMinestomInfo(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage(Component.translatable("minestom.info"));
    }
}
