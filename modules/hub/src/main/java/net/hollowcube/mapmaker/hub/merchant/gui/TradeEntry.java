package net.hollowcube.mapmaker.hub.merchant.gui;

import com.google.gson.JsonObject;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.hub.merchant.MerchantTrade;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.CostEntry;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeEntry extends View {

    private @ContextObject PlayerService playerService;
    private @ContextObject Player player;

    private @Outlet("btn") Label label;

    private final MerchantTrade trade;

    public TradeEntry(@NotNull Context context, @NotNull MerchantTrade trade) {
        super(context);
        this.trade = trade;

        var icon = trade.result().iconItem();
        var lore = new ArrayList<>(icon.get(DataComponents.LORE, List.of()));
        lore.add(Component.empty());
        trade.appendLore(PlayerDataV2.fromPlayer(player), PlayerBackpack.fromPlayer(player), lore);
        label.setItemDirect(icon.with(DataComponents.LORE, lore));
    }

    @Action(value = "btn", async = true)
    public void handleBuyItem() {
        if (!trade.canAfford(PlayerDataV2.fromPlayer(player), PlayerBackpack.fromPlayer(player)))
            return;

        Integer cubits = null, coins = null;
        Map<String, Integer> items = null;
        for (var entry : trade.inputs().entries().entrySet()) {
            var type = entry.getKey();
            if (type instanceof CostEntry.Coins)
                coins = entry.getValue();
            else if (type instanceof CostEntry.Cubits)
                cubits = entry.getValue();
            else if (type instanceof CostEntry.BackpackItem i) {
                if (items == null) items = new HashMap<>();
                items.put(i.entry().id(), -entry.getValue());
            }
        }

        JsonObject itemsObject = null;
        if (items != null && !items.isEmpty()) {
            itemsObject = AbstractHttpService.GSON.toJsonTree(items).getAsJsonObject();
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        playerService.buyCosmetic(playerData.id(), trade.result(), coins, cubits, itemsObject);

        var icon = trade.result().iconItem();
        player.sendMessage(Component.translatable("merchant.trade.success", icon.get(DataComponents.CUSTOM_NAME, Component.empty()).hoverEvent(icon.asHoverEvent())));
        player.closeInventory();
    }

}
