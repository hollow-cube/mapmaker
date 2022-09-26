package net.hollowcube.server.player;

import net.minestom.server.entity.Player;

public class MapMakerPlayer {
    // Database attributes
    private final int id;
    private final Player player;

    public MapMakerPlayer(int id, Player player) {
        this.id = id;
        this.player = player;
    }
}