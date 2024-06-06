package com.sk89q.worldedit.bukkit;

import org.bukkit.entity.Player;

public class BukkitAdapter {

    public static BukkitPlayer adapt(Player player) {
        return new BukkitPlayer(player.delegate());
    }
}
