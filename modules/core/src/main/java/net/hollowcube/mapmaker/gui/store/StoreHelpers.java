package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.store.ShopUpgradeCache;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

final class StoreHelpers {

    public enum Package {
        CUBITS_50,
        CUBITS_105,
        CUBITS_220,
        CUBITS_400,
        CUBITS_600,
        HYPERCUBE_1MO,
        HYPERCUBE_1Y
    }

    private static final String PURCHASE_SOURCE = "ingame/store";
    private static final BadSprite[] SPRITE_MAP = new BadSprite[Package.values().length];

    static {
        for (var packageName : Package.values()) {
            var sprite = "store/checkout/" + packageName.name().toLowerCase(Locale.ROOT);
            SPRITE_MAP[packageName.ordinal()] = BadSprite.require(sprite);
        }
    }

    static void buyPackage(@NotNull PlayerService playerService, @NotNull Player player, @NotNull Package packageName) {
        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var resp = playerService.createCheckoutLink(
                    PURCHASE_SOURCE, playerData.username(), packageName.name().toLowerCase(Locale.ROOT));

            var url = resp.url();
            if (ServerRuntime.getRuntime().isDevelopment()) {
                url = url.replace("https://hollowcube.net", "http://localhost:5173");
            }

            String finalUrl = url;
            player.scheduleNextTick(_ -> player.openBook(buildCheckoutBook(packageName.ordinal(), finalUrl)));
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.closeInventory();
            player.sendMessage(Component.text("An unknown error has occurred"));
        }
    }

    static boolean isUpgradeOwned(@NotNull Player player, @NotNull ShopUpgrade upgrade) {
        return ShopUpgradeCache.has(player, upgrade, true);
    }

    static void buyUpgrade(@NotNull PlayerService playerService, @NotNull PermManager permManager, @NotNull Player player, @NotNull ShopUpgrade upgrade) {
        if (ShopUpgradeCache.has(player, upgrade, true))
            return; // Sanity check

        // Ensure the player has enough cubits to buy the upgrade.
        var playerData = PlayerDataV2.fromPlayer(player);
        var backpack = PlayerBackpack.fromPlayer(player);
        if (!upgrade.canAfford(playerData, backpack)) {
            // Cannot afford, prompt to buy more cubits

            //todo
            player.sendMessage(Component.translatable("currency.missing"));
            return;
        }

        try {
            var meta = new JsonObject();
            meta.addProperty("source", "ingame/store");
            playerService.buyUpgrade(playerData.id(), upgrade.name().toLowerCase(Locale.ROOT), upgrade.cubits(), meta);

            // Success! Preempt the update message by updating locally
            playerData.setCubits(playerData.cubits() - upgrade.cubits());
            permManager.overwrite(upgrade.directPerm(), playerData.id(), true);
            permManager.overwrite(upgrade.indirectPerm(), playerData.id(), true);

            player.sendMessage(Component.translatable("store.add-ons.buy", Component.text(upgrade.name())));
        } catch (PlayerService.NotFoundError e) {
            player.sendMessage(Component.translatable("store.add-ons.buy.error"));
        } catch (Exception e) {
            player.sendMessage(Component.translatable("store.add-ons.buy.error"));
            ExceptionReporter.reportException(e, player);
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
