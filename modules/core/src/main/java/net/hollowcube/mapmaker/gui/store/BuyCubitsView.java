package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BuyCubitsView extends View {
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
        var playerData = PlayerDataV2.fromPlayer(player);
        var resp = playerService.createCheckoutLink(PURCHASE_SOURCE, playerData.id(), productIdMap[productIndex]);

        var background = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("booktest/buy_single"));
        var book = Component.text();
        book.append(Component.text("" + background.fontChar(), NamedTextColor.WHITE));
        book.appendNewline();

        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        book.appendNewline();
        for (int i = 0; i < 2; i++) {
            book.append(Component.text(FontUtil.computeOffset(16 + 5)));
            book.append(Component.text(FontUtil.computeOffset(83))
                    .hoverEvent(HoverEvent.showText(Component.text("Go to checkout")))
                    .clickEvent(ClickEvent.openUrl("https://hollowcube.net/store/checkout/fkelnk")));
            book.appendNewline();
        }

        player.openBook(Book.builder()
                .addPage(book.build())
                .build());

//        player.sendMessage("opening store link: " + resp.main().url());
    }
}
