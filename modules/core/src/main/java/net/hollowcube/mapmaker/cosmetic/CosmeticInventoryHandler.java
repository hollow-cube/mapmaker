package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.common.events.PlayerGiveCreativeItemEvent;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.gui.common.anvil.ColorPickerView;
import net.hollowcube.mapmaker.gui.store.CosmeticView;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CosmeticInventoryHandler {

    public static void init(@NotNull Controller guiController, @NotNull PlayerService playerService) {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(InventoryPreClickEvent.class, event -> handleInventoryCosmeticSelector(guiController, playerService, event));
        globalEventHandler.addListener(PlayerGiveCreativeItemEvent.class, CosmeticInventoryHandler::creativeClickListener);
    }

    private static void handleInventoryCosmeticSelector(@NotNull Controller guiController, @NotNull PlayerService playerService, @NotNull InventoryPreClickEvent event) {
        if (event.getInventory() != null) return; // Not the player inventory

        var cosmeticType = CosmeticType.byIconSlot(event.getSlot());
        if (cosmeticType == null) return;
        if (CosmeticView.DISABLED_TABS.contains(cosmeticType)) return;

        switch (event.getClickType()) {
            case RIGHT_CLICK -> openColorPicker(guiController, playerService, event.getPlayer(), cosmeticType);
            case LEFT_CLICK -> guiController.show(event.getPlayer(), c -> new CosmeticView(c, cosmeticType));
        }

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
            Map.entry((short) 8, CosmeticType.PARTICLE)
//            Map.entry((short) 45, CosmeticType.ACCESSORY)
    );

    private static final Tag<Integer> CREATIVE_LAST_SLOT_WIPE_HACK = Tag.Integer("creative_last_slot_wipe_hack").defaultValue(0);

    private static void creativeClickListener(@NotNull PlayerGiveCreativeItemEvent event) {
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
        MiscFunctionality.applyCosmetics(event.getPlayer(), PlayerDataV2.fromPlayer(event.getPlayer()));
        event.getPlayer().getInventory().setCursorItem(ItemStack.AIR);
        event.getPlayer().getInventory().update();

        // Show the cosmetic type selector
//        guiController.show(player, c -> new CosmeticView(c, cosmeticType));

        event.setCancelled(true);
    }

    private static void openColorPicker(
            @NotNull Controller guiController,
            @NotNull PlayerService playerService,
            @NotNull Player player,
            @NotNull CosmeticType cosmeticType
    ) {
        if (!CoreFeatureFlags.COLORABLE_COSMETICS.test(player)) return;

        var data = PlayerDataV2.fromPlayer(player);
        var options = data.getSetting(cosmeticType.setting());

        guiController.show(player, c -> ColorPickerView.builder()
                .callback(newColor -> {
                    var newOptions = options.withColor(newColor.asRGB());
                    data.setSetting(cosmeticType.setting(), newOptions);
                    MiscFunctionality.applyCosmetics(player, data);
                    FutureUtil.submitVirtual(() -> data.writeUpdatesUpstream(playerService));
                })
                .title("Select Color")
                .build(c, new AlphaColor(options.color()).withAlpha(0))
        );
    }

}
