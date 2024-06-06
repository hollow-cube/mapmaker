package org.bukkit;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.validate.Check;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Bukkit {

    public static Player getPlayer(UUID uuid) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
        Check.notNull(player, "Player not found");
        return new Player(player);
    }
}
