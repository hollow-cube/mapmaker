package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuyHypercubeView extends View {

    private static final String PURCHASE_SOURCE = "ingame/store";
    private static final String[] PRODUCT_ID_MAP = new String[]{
            "hypercube_1mo", "hypercube_1y"
    };
    private static final BadSprite[] SPRITE_MAP = new BadSprite[PRODUCT_ID_MAP.length];

    static {
        for (int i = 0; i < PRODUCT_ID_MAP.length; i++) {
            SPRITE_MAP[i] = BadSprite.require("store/checkout/" + PRODUCT_ID_MAP[i]);
        }
    }

    private @ContextObject PlayerService playerService;

    private @Outlet("buy_30d") Label buyHypercube30d;
    private @Outlet("buy_365d") Label buyHypercube365d;

    public BuyHypercubeView(@NotNull Context context) {
        super(context);
    }

    @Action("buy_30d")
    private void buyHypercube30d(@NotNull Player player) {
        BuyCubitsView.handleBuyCubitsGeneric(playerService, player, 5, buyHypercube30d);
    }

    @Action("buy_365d")
    private void buyHypercube365d(@NotNull Player player) {
        BuyCubitsView.handleBuyCubitsGeneric(playerService, player, 6, buyHypercube365d);
    }

}
