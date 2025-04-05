package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.graalvm.polyglot.HostAccess;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class StoreModule {
    private static final String PURCHASE_SOURCE = "ingame/store";
    private static final List<String> VALID_PACKAGES = List.of(
            "cubits_50", "cubits_105", "cubits_220",
            "cubits_400", "cubits_600", "hypercube_1mo",
            "hypercube_1y"
    );
    private static final BadSprite[] SPRITE_MAP = new BadSprite[VALID_PACKAGES.size()];

    static {
        for (int i = 0; i < VALID_PACKAGES.size(); i++) {
            SPRITE_MAP[i] = BadSprite.require("store/checkout/" + VALID_PACKAGES.get(i));
        }
    }

    private final PlayerService playerService;
    private final Player player;

    public StoreModule(@NotNull PlayerService playerService, @NotNull Player player) {
        this.playerService = playerService;
        this.player = player;
    }

    @HostAccess.Export
    public void buyPackage(@NotNull String packageName) {
        FutureUtil.submitVirtual(() -> buyPackage0(packageName));
    }

    @HostAccess.Export
    public boolean isUpgradeOwned(@NotNull String upgradeId) {
        System.out.println("isUpgradeUnlocked: " + upgradeId);
        return false;
    }

    @HostAccess.Export
    public void buyUpgrade(@NotNull String upgradeId) {
        System.out.println("buy upgrade: " + upgradeId);
    }

    @Blocking
    private void buyPackage0(@NotNull String packageName) {
        try {
            var packageIndex = VALID_PACKAGES.indexOf(packageName);
            if (packageIndex == -1) {
                throw new RuntimeException("Package '" + packageName + "' not found");
            }

            var playerData = PlayerDataV2.fromPlayer(player);
            var resp = playerService.createCheckoutLink(PURCHASE_SOURCE, playerData.username(), packageName);

            var url = resp.url();
            if (ServerRuntime.getRuntime().isDevelopment()) {
                url = url.replace("https://hollowcube.net", "http://localhost:5173");
            }

            String finalUrl = url;
            player.scheduleNextTick(_ -> player.openBook(buildCheckoutBook(packageIndex, finalUrl)));
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.closeInventory();
            player.sendMessage(Component.text("An unknown error has occurred"));
        }
    }

    private static @NotNull Book buildCheckoutBook(int productIndex, @NotNull String url) {
        var component = Component.text();

        component.append(Component.text(SPRITE_MAP[productIndex].fontChar(), TextColor.color(78, 92, 38)));

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
