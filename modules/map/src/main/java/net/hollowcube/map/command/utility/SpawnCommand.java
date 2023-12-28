package net.hollowcube.map.command.utility;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.command.CommandCategory;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class SpawnCommand extends Command {

    public SpawnCommand() {
        super("spawn", "tpstart");
        setCondition(mapFilter(true, true, true));
        
        category = CommandCategory.GLOBAL;

        addSyntax(playerOnly(this::handleTeleportToSpawn));
    }

    private void handleTeleportToSpawn(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0) {
            player.teleport(world.map().settings().getSpawnPoint());
            player.sendMessage(Component.translatable("teleport.spawn"));
        } else {
            player.sendMessage("idk how spawn works in playing maps"); //TODO
        }
    }

}
