package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.common.events.PlayerGiveCreativeItemEvent;
import net.hollowcube.mapmaker.gui.store.CosmeticPanel;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;

import java.util.Map;

public final class CosmeticEventHandler {

    private static final Map<Short, CosmeticType> COSMETIC_SLOT_MAP = Map.ofEntries(
            Map.entry((short) 5, CosmeticType.HAT),
            Map.entry((short) 6, CosmeticType.BACKWEAR),
            Map.entry((short) 7, CosmeticType.PET),
            Map.entry((short) 8, CosmeticType.PARTICLE)
            //            Map.entry((short) 45, CosmeticType.ACCESSORY)
    );

    public static void init(PlayerService players) {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(InventoryPreClickEvent.class, event -> handleInventoryCosmeticSelector(players, event));
        globalEventHandler.addListener(PlayerGiveCreativeItemEvent.class, CosmeticEventHandler::creativeClickListener);
    }

    private static void handleInventoryCosmeticSelector(PlayerService players, InventoryPreClickEvent event) {
        if (!(event.getInventory() instanceof PlayerInventory)) return; // Not the player inventory (e one, not just lower section)
        if (!(event.getClick() instanceof Click.Left(int slot))) return;

        var cosmeticType = CosmeticType.byIconSlot(slot);
        if (cosmeticType == null) return;
        if (CosmeticPanel.DISABLED_TABS.contains(cosmeticType)) return;
        Panel.open(event.getPlayer(), new CosmeticPanel(players, cosmeticType));
    }

    private static void creativeClickListener(PlayerGiveCreativeItemEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) return;
        var item = event.item();

        // If trying to set any slot to a cosmetic item
        if (item.hasTag(Cosmetic.COSMETIC_TAG)) {
            event.getPlayer().getInventory().update();
            return;
        }

        // Only care about cosmetic slots
        // We need to exclude any clicks that are setting it to a leather horse armor. The creative client handler resets it to the cmd item when reopening to sync.
        var cosmeticType = COSMETIC_SLOT_MAP.get(event.slot());
        if (cosmeticType == null || !item.isAir()) return; // Defer to the Minestom handling

        // Reset the inventory state
        MiscFunctionality.applyCosmetics(event.getPlayer(), PlayerData.fromPlayer(event.getPlayer()));
        event.getPlayer().getInventory().setCursorItem(ItemStack.AIR);
        event.getPlayer().getInventory().update();

        // Show the cosmetic type selector
//        guiController.show(player, c -> new CosmeticView(c, cosmeticType));

        event.setCancelled(true);
    }

    private CosmeticEventHandler() {
    }

}
