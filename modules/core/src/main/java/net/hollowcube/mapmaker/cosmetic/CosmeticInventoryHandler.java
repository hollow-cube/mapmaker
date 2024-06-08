package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.gui.store.CosmeticView;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.listener.CreativeInventoryActionListener;
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CosmeticInventoryHandler {

    public static void init(@NotNull Controller guiController) {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(InventoryPreClickEvent.class, event -> handleInventoryCosmeticSelector(guiController, event));
        var packetListenerManager = MinecraftServer.getPacketListenerManager();
        packetListenerManager.setPlayListener(ClientCreativeInventoryActionPacket.class, (packet, player) -> creativeClickListener(guiController, packet, player));
    }

    private static void handleInventoryCosmeticSelector(@NotNull Controller guiController, @NotNull InventoryPreClickEvent event) {
        if (event.getInventory() != null) return; // Not the player inventory

        var cosmeticType = CosmeticType.byIconSlot(event.getSlot());
        if (cosmeticType == null) return;
        if (CosmeticView.DISABLED_TABS.contains(cosmeticType)) return;

        guiController.show(event.getPlayer(), c -> new CosmeticView(c, cosmeticType));

//        if (event.getInventory() != event.getPlayerInventory())
//            return; // Not the player inventory (e one, not just lower section)
//
//        if (!(event.getClickInfo() instanceof Click.Info.Left leftClick)) return;
//
//        var cosmeticType = CosmeticType.byIconSlot(leftClick.slot());
//        if (cosmeticType == null) return;
//        if (CosmeticView.DISABLED_TABS.contains(cosmeticType)) return;
//
//        guiController.show(event.getPlayer(), c -> new CosmeticView(c, cosmeticType));
    }

    private static final Map<Short, CosmeticType> COSMETIC_SLOT_MAP = Map.ofEntries(
            Map.entry((short) 5, CosmeticType.HAT),
            Map.entry((short) 6, CosmeticType.BACKWEAR),
            Map.entry((short) 7, CosmeticType.PET),
            Map.entry((short) 8, CosmeticType.PARTICLE),
            Map.entry((short) 45, CosmeticType.ACCESSORY)
    );

    private static void creativeClickListener(@NotNull Controller guiController, ClientCreativeInventoryActionPacket packet, Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) return;
        var item = packet.item();

        // If trying to set any slot to a cosmetic item
        if (item.hasTag(Cosmetic.COSMETIC_TAG)) {
            player.getInventory().update();
            return;
        }

        // Only care about cosmetic slots
        // We need to exclude any clicks that are setting it to a leather horse armor. The creative client handler resets it to the cmd item when reopening to sync.
        var cosmeticType = COSMETIC_SLOT_MAP.get(packet.slot());
        if (cosmeticType == null || !item.isAir()) {
            // Defer to the Minestom handling
            CreativeInventoryActionListener.listener(packet, player);
            return;
        }

        // Reset the inventory state
        MiscFunctionality.applyCosmetics(player, PlayerDataV2.fromPlayer(player));
        player.getInventory().setCursorItem(ItemStack.AIR);
        player.getInventory().update();

        // Show the cosmetic type selector
        guiController.show(player, c -> new CosmeticView(c, cosmeticType));
    }

}
