package net.hollowcube.server.commands.map;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import static omega.mapmaker.events.once.CreateMapEvent.onCreateMap;

public class JoinMapCommand extends Command {
    public JoinMapCommand() {
        super("join");

        setDefaultExecutor(((sender, context) -> {
            sender.sendMessage("Usage: /join <map-name>");
        }));

        var mapNameArg = ArgumentType.String("map-name");

        addSyntax((sender, context) -> {
            Player player = (Player) sender;
            final String mapName = context.get(mapNameArg);
            onCreateMap(player, mapName);
        }, mapNameArg);
    }
}
