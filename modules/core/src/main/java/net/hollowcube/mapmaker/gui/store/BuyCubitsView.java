package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class BuyCubitsView extends View {
    private static final Logger log = LoggerFactory.getLogger(BuyCubitsView.class);

    private static final String PURCHASE_SOURCE = "ingame/store";
    private static final String[] productIdMap = new String[]{
            "cubits_50", "cubits_105", "cubits_220",
            "cubits_400", "cubits_600"
    };

    private @ContextObject PlayerService playerService;

    private @OutletGroup("buy_.+") Label[] buyCubitsButtons;

    public BuyCubitsView(@NotNull Context context) {
        super(context);

        for (int i = 0; i < buyCubitsButtons.length; i++) {
            var productIndex = i;
            addAsyncActionHandler(
                    Objects.requireNonNull(buyCubitsButtons[i].id()),
                    Label.ActionHandler.lmb(player -> handleBuyCubitsGeneric(player, productIndex))
            );
        }
    }

    @Blocking
    private void handleBuyCubitsGeneric(@NotNull Player player, int productIndex) {
        // We cannot handle clicks in books serverside in addition to opening a website so we can do the following:
        // 1. Instead give our own link with the checkout id such as https://api.hollowcube.net/v1/players/checkout/b18925t71892578912579157125897128951851
        //    this link would then go to the player service, triggering a kafka message which the server reads and
        //    uses to close the GUI.
        // 2. This looks kinda yikes, so we can shorten the link with a little link shortener cf worker, so it would be more like
        //    https://s.hollowcube.net/faw512b -> https://api.hollowcube.net/v1/players/checkout/b18925t71892578912579157125897128951851 -> https://checkout.tebex.io/checkout/...
        //    and at the same time we can send the kafka update when stopping at the player service.
        buyCubitsButtons[productIndex].setState(State.LOADING);
        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var resp = playerService.createCheckoutLink(PURCHASE_SOURCE, playerData.username(), productIdMap[productIndex]);
            log.info("Created checkout link for {} for product {}: {}", playerData.username(), productIdMap[productIndex], resp.url());

            var book = Component.text();
            book.append(Component.text("Checkout Button")
                    .hoverEvent(HoverEvent.showText(Component.text("Go to checkout")))
                    .clickEvent(ClickEvent.openUrl(resp.url())));
            player.openBook(Book.builder()
                    .addPage(book.build())
                    .build());
        } catch (Exception e) {
            log.error("Failed to create checkout link", e);
            player.closeInventory();
            player.sendMessage(Component.text("An unknown error has occurred"));
        } finally {
            buyCubitsButtons[productIndex].setState(State.ACTIVE);
        }
    }
}
