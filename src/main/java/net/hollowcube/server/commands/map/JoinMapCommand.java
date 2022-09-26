package net.hollowcube.server.commands.map;

import net.hollowcube.server.events.once.JoinMapEvent;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class JoinMapCommand extends Command {
    public JoinMapCommand() {
        super("join");

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /join <map-name>"));

        var mapNameArg = ArgumentType.String("map-name");

        addSyntax((sender, context) -> {
            if(sender instanceof Player player) {
                final String mapName = context.get(mapNameArg);
                JoinMapEvent.onJoinMap(player, mapName);
            } else {
                sender.sendMessage("Console cannot use this command!");
            }
        }, mapNameArg);
    }
}
