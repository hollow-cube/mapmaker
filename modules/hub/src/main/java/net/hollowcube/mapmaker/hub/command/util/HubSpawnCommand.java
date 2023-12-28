package net.hollowcube.mapmaker.hub.command.util;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.hollowcube.mapmaker.hub.HubServerBase;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubSpawnCommand extends Command {

    private final HubServerBase hubServerBase;

    public HubSpawnCommand(HubServerBase hubServerBase) {
        super("spawn", "hub");
        this.hubServerBase = hubServerBase;
        
        category = CommandCategory.GLOBAL;

        addSyntax(playerOnly(this::handleSpawn));
    }

    private void handleSpawn(@NotNull Player player, @NotNull CommandContext context) {
        hubServerBase.teleportToSpawn(player);
    }
}
