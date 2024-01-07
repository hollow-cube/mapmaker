package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MinestomCommand extends CommandDsl {
    public MinestomCommand() {
        super("minestom", "pl");

        addSyntax(this::sendMinestomInfo);
    }

    private void sendMinestomInfo(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage(Component.text()
                .append(Component.text("todo format me better + add link + etc")).appendNewline()
                .append(Component.text("We are using Minestom!")));
    }
}
