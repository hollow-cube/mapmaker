package net.hollowcube.server.player;

import net.hollowcube.server.util.gui.inventory.InventoryUtils;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MapMakerPlayerLoader {
    private final List<String> loadingPlayers;

    public MapMakerPlayerLoader() {
        this.loadingPlayers = new ArrayList<>();
    }

    public void loadMapMakerPlayer(final Player player) {
        InventoryUtils.setPlayerLobbyInventory(player);
    }
}
