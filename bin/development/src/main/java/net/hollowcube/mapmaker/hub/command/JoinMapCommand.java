package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.server.events.once.JoinMapEvent;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

/*
Creating a map is a two step process:
- Create the entry in database and update player data
- Spawn the player in the map

Hub should have some interface to send players to maps
- dev module will implement a bridge to the map creation side


create a map -> storage
join a map -> server switch


 */

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
