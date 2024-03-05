package net.hollowcube.mapmaker.hub.merchant.gui;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.hub.merchant.MerchantTrade;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class TradeEntry extends View {

    private @ContextObject Player player;

    private @Outlet("btn") Label label;

    private final MerchantTrade trade;

    public TradeEntry(@NotNull Context context, @NotNull MerchantTrade trade) {
        super(context);
        this.trade = trade;

        var icon = trade.result().icon();
        var lore = new ArrayList<>(icon.getLore());
        lore.add(Component.empty());
        trade.appendLore(PlayerDataV2.fromPlayer(player), PlayerBackpack.fromPlayer(player), lore);
        label.setItemDirect(icon.withLore(lore));
    }

    @Action("btn")
    public void handleBuyItem() {
        if (!trade.canAfford(PlayerDataV2.fromPlayer(player), PlayerBackpack.fromPlayer(player)))
            return;

        player.closeInventory();
        player.sendMessage("tried to buy something but it isnt implemented :(");
    }


}
