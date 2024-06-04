package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class BuyCubitsView extends View {
    private static final Logger log = LoggerFactory.getLogger(BuyCubitsView.class);

    private static final String PURCHASE_SOURCE = "ingame/store";
    private static final String[] productIdMap = new String[]{
            "cubits_50", "cubits_105", "cubits_220",
            "cubits_400", "cubits_600"
    };
    private static final BadSprite[] spriteMap = new BadSprite[productIdMap.length];

    static {
        for (int i = 0; i < productIdMap.length; i++) {
            spriteMap[i] = BadSprite.require("store/checkout/" + productIdMap[i]);
        }
    }

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
        buyCubitsButtons[productIndex].setState(State.LOADING);
        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var resp = playerService.createCheckoutLink(PURCHASE_SOURCE, playerData.username(), productIdMap[productIndex]);
            log.info("Created checkout link for {} for product {}: {}", playerData.username(), productIdMap[productIndex], resp.url());

            var url = resp.url();
            if (ServerRuntime.getRuntime().isDevelopment()) {
                url = url.replace("https://hollowcube.net", "http://localhost:5173");
            }
            
            player.openBook(buildCheckoutBook(productIndex, url));
        } catch (Exception e) {
            log.error("Failed to create checkout link", e);
            player.closeInventory();
            player.sendMessage(Component.text("An unknown error has occurred"));
        } finally {
            buyCubitsButtons[productIndex].setState(State.ACTIVE);
        }
    }

    private @NotNull Book buildCheckoutBook(int productIndex, @NotNull String url) {
        var component = Component.text();

        component.append(Component.text(spriteMap[productIndex].fontChar(), TextColor.color(78, 92, 38)));

        component.appendNewline().appendNewline().appendNewline().appendNewline();

        var line = Component.text(FontUtil.computeOffset(10)).append(Component.text(FontUtil.computeOffset(94))
                .hoverEvent(HoverEvent.showText(LanguageProviderV2.translateMultiMerged("store.checkout.open_in_browser", List.of())))
                .clickEvent(ClickEvent.openUrl(url)));
        for (int i = 0; i < 9; i++) {
            component.append(line).appendNewline();
        }

        return Book.builder().addPage(component.build()).build();
    }
}
