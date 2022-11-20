package net.hollowcube.mapmaker.gui.inventory;

import net.hollowcube.mapmaker.gui.item.ItemUtils;
import net.minestom.server.entity.Player;

public class InventoryUtils {
    public static void setPlayerLobbyInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setItemStack(0, ItemUtils.GUI_PLAY_MAPS);
        player.getInventory().setItemStack(1, ItemUtils.GUI_CREATE_MAPS);
    }
}
