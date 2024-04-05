package net.hollowcube.mapmaker.hub.merchant.gui;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.hub.merchant.MerchantData;
import net.hollowcube.mapmaker.hub.merchant.MerchantTrade;
import org.jetbrains.annotations.NotNull;

public class MerchantShopView extends View {
    private final MerchantData data;

    public MerchantShopView(@NotNull Context context, @NotNull MerchantData data) {
        super(context);
        this.data = data;
    }

    @Action("trade_list")
    public void createTradeList(Pagination.@NotNull PageRequest<TradeEntry> request) {
        var result = data.trades().stream()
                .sorted(Cosmetic.comparingName(MerchantTrade::result))
                .sorted(Cosmetic.comparingRarity(MerchantTrade::result))
                .map(trade -> new TradeEntry(request.context(), trade))
                .toList();
        request.respond(result, false);
    }
}
