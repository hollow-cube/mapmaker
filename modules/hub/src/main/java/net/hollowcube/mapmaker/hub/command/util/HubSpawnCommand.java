package net.hollowcube.mapmaker.hub.command.util;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubSpawnCommand extends CommandDsl {
    private final HubMapWorld world;

    @Inject
    public HubSpawnCommand(@NotNull HubMapWorld world) {
        super("spawn", "hub");
        this.world = world;

        category = CommandCategory.GLOBAL;

        addSyntax(playerOnly(this::handleSpawn));
    }

    private void handleSpawn(@NotNull Player player, @NotNull CommandContext context) {
        player.teleport(world.spawnPoint(player));
    }
}
