package net.hollowcube.server.util.gui.inventory;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import omega.mapmaker.util.gui.item.ItemUtils;

public class InventoryUtils {
    public static void setPlayerLobbyInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setItemStack(0, ItemUtils.GUI_PLAY_MAPS);
        player.getInventory().setItemStack(1, ItemUtils.GUI_CREATE_MAPS);
    }
}
