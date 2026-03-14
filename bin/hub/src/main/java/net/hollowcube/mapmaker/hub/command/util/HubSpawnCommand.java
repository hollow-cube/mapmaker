package net.hollowcube.mapmaker.hub.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;

public class HubSpawnCommand extends CommandDsl {

    public HubSpawnCommand() {
        super("spawn", "hub");

        category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::handleSpawn));
    }

    private void handleSpawn(Player player, CommandContext context) {
        player.teleport(HubMapWorld.spawnPointFor(player), Vec.ZERO, null, RelativeFlags.NONE);
    }
}
