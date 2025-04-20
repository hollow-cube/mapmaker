package net.hollowcube.mapmaker.hub.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.NotNull;

public class HubSpawnCommand extends CommandDsl {
    private final HubMapWorld world;

    public HubSpawnCommand(@NotNull HubMapWorld world) {
        super("spawn", "hub");
        this.world = world;

        category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::handleSpawn));
    }

    private void handleSpawn(@NotNull Player player, @NotNull CommandContext context) {
        player.teleport(world.spawnPoint(player), Vec.ZERO, null, RelativeFlags.NONE);
    }
}
