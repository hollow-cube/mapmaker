package net.hollowcube.mapmaker.hub.merchant.gui;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.merchant.MerchantData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MerchantShopView extends View {
    private final MerchantData data;

    public MerchantShopView(@NotNull Context context, @NotNull MerchantData data) {
        super(context);
        this.data = data;
    }

    @Action("trade_list")
    public void createTradeList(Pagination.@NotNull PageRequest<TradeEntry> request) {
        var entries = new ArrayList<TradeEntry>();
        for (var trade : data.trades()) {
            entries.add(new TradeEntry(request.context(), trade));
        }

        request.respond(entries, false);
    }
}
