package net.hollowcube.mapmaker.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class SpawnCommand extends CommandDsl {

    public SpawnCommand() {
        super("spawn", "tpstart");
        setCondition(mapFilter(true, true, true, true));

        this.description = "Teleports you to the spawn location of the world you are in";
        this.category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::handleTeleportToSpawn));
    }

    private void handleTeleportToSpawn(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;

        // If the world is an editor OR the player is spectating OR the world is a building map, just teleport them to the spawn.
        if (world.canEdit(player) || world.isSpectating(player) || world.map().settings().getVariant() == MapVariant.BUILDING) {
            MapWorldHelpers.teleportPlayer(player, world.spawnPoint(player));
            player.sendMessage(Component.translatable("teleport.spawn"));
        } else if (world.map().settings().getVariant() == MapVariant.PARKOUR && world.isPlaying(player)) {
            // If it is a parkour world and they are playing, reset them
            EventDispatcher.call(new MapPlayerResetEvent(player, world, false));
        }
    }

}
