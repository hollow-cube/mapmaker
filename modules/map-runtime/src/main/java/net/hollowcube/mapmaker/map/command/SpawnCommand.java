package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

public class SpawnCommand extends CommandDsl {

    public SpawnCommand() {
        super("spawn", "tpstart");

        this.description = "Teleports you to the spawn location of the world you are in";
        this.category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::handleTeleportToSpawn));
    }

    private void handleTeleportToSpawn(Player player, CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (world == null) return;

        if (world instanceof ParkourMapWorld pkWorld && pkWorld.getPlayerState(player) instanceof ParkourState.AnyPlaying) {
            pkWorld.hardResetPlayer(player);
        } else {
            MapWorldHelpers.teleportPlayer(player, world.map().settings().getSpawnPoint());
            player.sendMessage(Component.translatable("teleport.spawn"));
        }
    }

}
