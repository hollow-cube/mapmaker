package com.thevoxelbox.voxelsniper.sniper;

import net.minestom.server.entity.Player;

import java.util.UUID;

public record Sniper(
        Player player
) {

    public UUID getUuid() {
        return player.getUuid();
    }
}
