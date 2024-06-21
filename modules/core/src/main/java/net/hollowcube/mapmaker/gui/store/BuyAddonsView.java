package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.store.ShopUpgradeCache;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class BuyAddonsView extends View {

    private @ContextObject PlayerService playerService;
    private @ContextObject PermManager permManager;
    private @ContextObject Player player;

    private @Outlet("terraform_switch") Switch terraformSwitch;
    private @Outlet("map_slots_switch") Switch mapSlotsSwitch;
    // Boosts
    // Folders
    private @Outlet("map_size_switch") Switch mapSizeSwitch;
    // Personal world

    public BuyAddonsView(@NotNull Context context) {
        super(context);

        terraformSwitch.setOption(ShopUpgradeCache.has(player, ShopUpgrade.BUILD_TOOLS, true));

        if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SLOT_3, true)) {
            mapSlotsSwitch.setOption(0);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SLOT_4, true)) {
            mapSlotsSwitch.setOption(1);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SLOT_5, true)) {
            mapSlotsSwitch.setOption(2);
        } else {
            mapSlotsSwitch.setOption(3);
        }

        if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SIZE_2, true)) {
            mapSizeSwitch.setOption(0);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SIZE_3, true)) {
            mapSizeSwitch.setOption(1);
        } else if (!ShopUpgradeCache.has(player, ShopUpgrade.MAP_SIZE_4, true)) {
            mapSizeSwitch.setOption(2);
        } else {
            mapSizeSwitch.setOption(3);
        }
    }

    @Action(value = "terraform_advanced", async = true)
    private void handleBuildTools() {
        //submitUpgradePurchase(ShopUpgrade.BUILD_TOOLS);
    }

    @Action(value = "map_slots_3", async = true)
    private void handleMapSlot3() {
        submitUpgradePurchase(ShopUpgrade.MAP_SLOT_3);
    }

    @Action(value = "map_slots_4", async = true)
    private void handleMapSlot4() {
        submitUpgradePurchase(ShopUpgrade.MAP_SLOT_4);
    }

    //todo boosts

    //todo folders

    @Action(value = "map_size_large", async = true)
    private void handleMapSizeLarge() {
        submitUpgradePurchase(ShopUpgrade.MAP_SIZE_2);
    }

    @Action(value = "map_size_massive", async = true)
    private void handleMapSizeMassive() {
        submitUpgradePurchase(ShopUpgrade.MAP_SIZE_3);
    }

    @Action(value = "map_size_colossal", async = true)
    private void handleMapSizeColossal() {
        submitUpgradePurchase(ShopUpgrade.MAP_SIZE_4);
    }

    //todo personal worlds

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

        //todo this should have a confirm gui, maybe with some extra details
        try {
            var meta = new JsonObject();
            meta.addProperty("source", "ingame/store");
            playerService.buyUpgrade(playerData.id(), upgrade.name().toLowerCase(Locale.ROOT), upgrade.cubits(), meta);

            // Success! Preempt the update message by updating locally
            playerData.setCubits(playerData.cubits() - upgrade.cubits());
            permManager.overwrite(upgrade.directPerm(), playerData.id(), true);
            permManager.overwrite(upgrade.indirectPerm(), playerData.id(), true);

            player.closeInventory();
            player.sendMessage("You unlocked " + upgrade.name() + "!");
        } catch (PlayerService.NotFoundError e) {
            player.sendMessage("todo player not found");
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage("todo something went wrong");
        }
    }

}