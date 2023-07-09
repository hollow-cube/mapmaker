package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends BaseMapCommand {
    public SpawnCommand() {
        super(false, "spawn", "tpstart");

        setDefaultExecutor(this::teleportToSpawn);
    }

    private void teleportToSpawn(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var world = MapWorld.forPlayer(player);
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0) {
            player.teleport(world.map().settings().getSpawnPoint());
            player.sendMessage("Teleported to spawn");
        } else {
            player.sendMessage("idk how spawn works in playing maps");
        }
    }

}
