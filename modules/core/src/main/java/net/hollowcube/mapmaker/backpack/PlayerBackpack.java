package net.hollowcube.mapmaker.backpack;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientPlaceRecipePacket;
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket;
import net.minestom.server.network.packet.server.play.RecipeBookAddPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;

import java.util.Arrays;
import java.util.EnumMap;

public class PlayerBackpack {
    private static final PlayerSetting<Boolean> IS_BACKPACK_OPEN = PlayerSetting.Bool("backpack_open", false);
    private static final PlayerSetting<Boolean> IS_BACKPACK_FILTERED = PlayerSetting.Bool("backpack_filtered", false);

    public static final Tag<PlayerBackpack> TAG = Tag.Transient("mapmaker:player_backpack");

    public static PlayerBackpack fromPlayer(Player player) {
        return player.getTag(TAG);
    }

    static {
        var packetListenerManager = MinecraftServer.getPacketListenerManager();
        packetListenerManager.setPlayListener(ClientPlaceRecipePacket.class, PlayerBackpack::handleRecipeBookClick);
        packetListenerManager.setPlayListener(ClientSetRecipeBookStatePacket.class, PlayerBackpack::handleSetRecipeBookState);
    }

    private final Player player;
    private final EnumMap<BackpackItem, Integer> contents = new EnumMap<>(BackpackItem.class);

    public PlayerBackpack(Player player) {
        this.player = player;
    }

    public int getQuantity(BackpackItem item) {
        return contents.getOrDefault(item, 0);
    }

    /**
     * Does NOT send to the player automatically.
     */
    public void update(JsonObject networkObject) {
        for (var item : BackpackItem.values()) {
            var key = item.name().toLowerCase();
            if (networkObject.has(key)) {
                contents.put(item, networkObject.get(key).getAsInt());
            }
        }
    }

    /**
     * Sends the backpack to the player
     */
    public void refresh() {
        var recipeBookPacket = new RecipeBookAddPacket(
            Arrays.stream(BackpackItem.values())
                .map(cm -> cm.getRecipeBookEntry(getQuantity(cm))).toList(), true
        );
        player.sendPacket(recipeBookPacket);

        // TODO: 1.21.2 (RecipeBookSettingsPacket)
//        var playerData = PlayerDataV2.fromPlayer(player);
//        var unlockRecipesPacket = new UnlockRecipesPacket(0,
//            playerData.getSetting(IS_BACKPACK_OPEN), playerData.getSetting(IS_BACKPACK_FILTERED),
//            false, false,
//            false, false,
//            false, false,
//            Arrays.stream(BackpackItem.values()).map(BackpackItem::recipeBookId).toList(), List.of());
//        player.sendPacket(unlockRecipesPacket);
    }

    private static void handleRecipeBookClick(ClientPlaceRecipePacket packet, Player player) {
        // Remove the ghost recipe
        player.getInventory().setItemStack(PlayerInventoryUtils.CRAFT_RESULT, RecipeBookHack.BLANK_ITEM_CRAFTABLE);
    }

    private static void handleSetRecipeBookState(ClientSetRecipeBookStatePacket packet, Player player) {
        if (packet.bookType() != ClientSetRecipeBookStatePacket.BookType.CRAFTING) return;

        var playerData = PlayerData.fromPlayer(player);
        playerData.setSetting(IS_BACKPACK_OPEN, packet.bookOpen());
        playerData.setSetting(IS_BACKPACK_FILTERED, packet.filterActive());
        // No need to write updates, the player data will be saved when leaving the hub
    }

}
