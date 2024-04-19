package net.hollowcube.mapmaker.backpack;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientCraftRecipeRequest;
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.network.packet.server.play.UnlockRecipesPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PlayerBackpack {
    private static final PlayerSetting<Boolean> IS_BACKPACK_OPEN = PlayerSetting.Bool("backpack_open", false);
    private static final PlayerSetting<Boolean> IS_BACKPACK_FILTERED = PlayerSetting.Bool("backpack_filtered", false);

    public static final Tag<PlayerBackpack> TAG = Tag.Transient("mapmaker:player_backpack");

    public static @NotNull PlayerBackpack fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    static {
        var packetListenerManager = MinecraftServer.getPacketListenerManager();
        packetListenerManager.setPlayListener(ClientCraftRecipeRequest.class, PlayerBackpack::handleRecipeBookClick);
        packetListenerManager.setPlayListener(ClientSetRecipeBookStatePacket.class, PlayerBackpack::handleSetRecipeBookState);
    }

    private final Player player;
    private final EnumMap<BackpackItem, Integer> contents = new EnumMap<>(BackpackItem.class);

    public PlayerBackpack(@NotNull Player player) {
        this.player = player;
    }

    public int getQuantity(@NotNull BackpackItem item) {
        return contents.getOrDefault(item, 0);
    }

    /**
     * Does NOT send to the player automatically.
     */
    public void update(@NotNull JsonObject networkObject) {
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
        var declareRecipesPacket = new DeclareRecipesPacket(Arrays.stream(BackpackItem.values())
                .map(cm -> cm.getRecipePlaceholder(getQuantity(cm))).toList());
//        player.sendPacket(declareRecipesPacket);

        // Having an empty second list stops the weird expanding animation. it seems like wikivg is just wrong about this.
        var playerData = PlayerDataV2.fromPlayer(player);
        var unlockRecipesPacket = new UnlockRecipesPacket(0,
                playerData.getSetting(IS_BACKPACK_OPEN), playerData.getSetting(IS_BACKPACK_FILTERED),
                false, false,
                false, false,
                false, false,
                Arrays.stream(BackpackItem.values()).map(BackpackItem::recipeBookId).toList(), List.of());
//        player.sendPacket(unlockRecipesPacket);

        player.getInventory().setItemStack(9, RecipeBookHack.BLANK_ITEM_CRAFTABLE);
    }

    private static void handleRecipeBookClick(@NotNull ClientCraftRecipeRequest packet, @NotNull Player player) {
        // Remove the ghost recipe
        player.getInventory().setItemStack(PlayerInventoryUtils.CRAFT_RESULT, RecipeBookHack.BLANK_ITEM_CRAFTABLE);
    }

    private static void handleSetRecipeBookState(@NotNull ClientSetRecipeBookStatePacket packet, @NotNull Player player) {
        if (packet.bookType() != ClientSetRecipeBookStatePacket.BookType.CRAFTING) return;

        var playerData = PlayerDataV2.fromPlayer(player);
        playerData.setSetting(IS_BACKPACK_OPEN, packet.bookOpen());
        playerData.setSetting(IS_BACKPACK_FILTERED, packet.filterActive());
        // No need to write updates, the player data will be saved when leaving the hub
    }

}
