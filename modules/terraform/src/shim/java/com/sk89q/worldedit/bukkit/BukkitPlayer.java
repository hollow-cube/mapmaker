package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.util.Location;
import net.minestom.server.entity.Player;

public record BukkitPlayer(Player player) {

    public Location getLocation() {
        return new Location(player.getPosition());
    }
}
