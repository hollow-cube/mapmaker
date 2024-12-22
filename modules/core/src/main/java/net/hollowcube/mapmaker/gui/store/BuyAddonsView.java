package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.gui.common.ConfirmAction;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.store.ShopUpgradeCache;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuyAddonsView extends View {

    private @ContextObject PlayerService playerService;
    private @ContextObject PermManager permManager;
    private @ContextObject Player player;

    private @Outlet("terraform_switch") Switch terraformSwitch;
    private @Outlet("map_slots_switch") Switch mapSlotsSwitch;
    private @Outlet("map_slots_cost") Switch mapSlotsCost;
    // Boosts
    private @Outlet("map_size_switch") Switch mapSizeSwitch;
    private @Outlet("map_size_cost") Switch mapSizeCost;

    private @OutletGroup("buy_.+") Label[] buyableEntries;

    public BuyAddonsView(@NotNull Context context) {
        super(context);

        terraformSwitch.setOption(ShopUpgradeCache.has(player, ShopUpgrade.BUILD_TOOLS, true));

        if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SLOT_3, true)) {
            mapSlotsSwitch.setOption(0);
            mapSlotsCost.setOption(0);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SLOT_4, true)) {
            mapSlotsSwitch.setOption(1);
            mapSlotsCost.setOption(1);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SLOT_5, true)) {
            mapSlotsSwitch.setOption(2);
            mapSlotsCost.setOption(2);
        } else {
            mapSlotsSwitch.setOption(3);
            mapSlotsCost.setOption(3);
        }

        if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SIZE_2, true)) {
            mapSizeSwitch.setOption(0);
            mapSizeCost.setOption(0);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SIZE_3, true)) {
            mapSizeSwitch.setOption(1);
            mapSizeCost.setOption(1);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SIZE_4, true)) {
            mapSizeSwitch.setOption(2);
            mapSizeCost.setOption(2);
        } else {
            mapSizeSwitch.setOption(3);
            mapSizeCost.setOption(3);
        }

        for (var entry : buyableEntries) {
            var upgrade = ShopUpgrade.valueOf(entry.id().substring(4).toUpperCase(Locale.ROOT));

            var itemStack = entry.getItemDirect();
            var lore = new ArrayList<>(itemStack.get(ItemComponent.LORE, List.of()));
            lore.add(Component.empty());

            upgrade.appendLore(PlayerDataV2.fromPlayer(player), PlayerBackpack.fromPlayer(player), lore);
            entry.setItemDirect(itemStack.with(ItemComponent.LORE, lore));
        }
    }

    @Action(value = "terraform_advanced", async = true)
    private void handleBuildTools() {
        // Not supported yet.
        //submitUpgradePurchase(ShopUpgrade.BUILD_TOOLS);
    }

    @Action(value = "buy_map_slot_3", async = true)
    private void handleMapSlot3() {
        submitUpgradePurchase(ShopUpgrade.MAP_SLOT_3);
    }

    @Action(value = "buy_map_slot_4", async = true)
    private void handleMapSlot4() {
        submitUpgradePurchase(ShopUpgrade.MAP_SLOT_4);
    }

    @Action(value = "buy_map_slot_5", async = true)
    private void handleMapSlot5() {
        submitUpgradePurchase(ShopUpgrade.MAP_SLOT_5);
    }

    //todo boosts

    @Action(value = "buy_map_size_2", async = true)
    private void handleMapSizeLarge() {
        submitUpgradePurchase(ShopUpgrade.MAP_SIZE_2);
    }

    @Action(value = "buy_map_size_3", async = true)
    private void handleMapSizeMassive() {
        submitUpgradePurchase(ShopUpgrade.MAP_SIZE_3);
    }

    @Action(value = "map_size_colossal", async = true)
    private void handleMapSizeColossal() {
        // Not supported yet.
//        submitUpgradePurchase(ShopUpgrade.MAP_SIZE_4);
    }

    private void submitUpgradePurchase(@NotNull ShopUpgrade upgrade) {
        if (ShopUpgradeCache.has(player, upgrade, true))
            return; // Sanity check

        var playerData = PlayerDataV2.fromPlayer(player);
        var backpack = PlayerBackpack.fromPlayer(player);
        if (!upgrade.canAfford(playerData, backpack)) {
            // Cannot afford, prompt to buy more cubits

            //todo
            player.closeInventory();
            player.sendMessage(Component.translatable("currency.missing"));
            return;
        }

        pushView(context -> new ConfirmAction(context, () -> tryUpgradePurchase(upgrade),
                Component.translatable("purchase this upgrade for " + upgrade.cubits() + " Cubits")));

    }

    private void tryUpgradePurchase(@NotNull ShopUpgrade upgrade) {
        try {
            var playerData = PlayerDataV2.fromPlayer(player);

            var meta = new JsonObject();
            meta.addProperty("source", "ingame/store");
            playerService.buyUpgrade(playerData.id(), upgrade.name().toLowerCase(Locale.ROOT), upgrade.cubits(), meta);

            // Success! Preempt the update message by updating locally
            playerData.setCubits(playerData.cubits() - upgrade.cubits());
            permManager.overwrite(upgrade.directPerm(), playerData.id(), true);
            permManager.overwrite(upgrade.indirectPerm(), playerData.id(), true);

            player.closeInventory();
            player.sendMessage(Component.translatable("store.add-ons.buy", Component.text(upgrade.name())));
        } catch (PlayerService.NotFoundError e) {
            player.sendMessage(Component.translatable("store.add-ons.buy.error"));
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(Component.translatable("store.add-ons.buy.error"));
        }
    }

}