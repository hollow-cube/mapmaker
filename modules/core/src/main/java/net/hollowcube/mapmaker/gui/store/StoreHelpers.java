package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.dialogs.DialogBuilder;
import net.hollowcube.common.dialogs.DialogButtons;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class StoreHelpers {

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

    public static void buyPackage(@NotNull PlayerService playerService, @NotNull Player player, @NotNull Package packageName) {
        try {
            var playerData = PlayerData.fromPlayer(player);
            var resp = playerService.createCheckoutLink(
                PURCHASE_SOURCE, playerData.username(), packageName.name().toLowerCase(Locale.ROOT));

            var url = resp.url();
            if (ServerRuntime.getRuntime().isDevelopment()) {
                url = url.replace("https://hollowcube.net", "http://localhost:5173");
            }

            String finalUrl = url;
            player.scheduleNextTick(_ -> {
                player.closeInventory();
                player.sendPacket(new ShowDialogPacket(buildCheckoutDialog(packageName.ordinal(), finalUrl)));
            });
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.closeInventory();
            player.sendMessage(Component.text("An unknown error has occurred"));
        }
    }

    static boolean isUpgradeOwned(@NotNull Player player, @NotNull ShopUpgrade upgrade) {
        return upgrade.has(PlayerData.fromPlayer(player));
    }

    public static void buyUpgrade(@NotNull PlayerService playerService, @NotNull Player player, @NotNull ShopUpgrade upgrade) {
        if (isUpgradeOwned(player, upgrade))
            return; // Sanity check

        // Ensure the player has enough cubits to buy the upgrade.
        var playerData = PlayerData.fromPlayer(player);
        var backpack = PlayerBackpack.fromPlayer(player);
        if (!upgrade.canAfford(playerData, backpack)) {
            // Cannot afford, prompt to buy more cubits

            //todo
            player.sendMessage(Component.translatable("currency.missing"));
            player.closeInventory();
            return;
        }

        try {
            var meta = new JsonObject();
            meta.addProperty("source", "ingame/store");
            playerService.buyUpgrade(playerData.id(), upgrade.name().toLowerCase(Locale.ROOT), upgrade.cubits(), meta);

            // Success! Preempt the update message by updating locally
            playerData.setCubits(playerData.cubits() - upgrade.cubits());
            playerData.updateFromMapUpgrade(upgrade.mapSlots(), upgrade.maxMapSize(), upgrade.mapBuilders());

            player.sendMessage(upgrade.buyComponent());
        } catch (PlayerService.NotFoundError e) {
            player.sendMessage(Component.translatable("store.add-ons.buy.error"));
            player.closeInventory();
        } catch (Exception e) {
            player.sendMessage(Component.translatable("store.add-ons.buy.error"));
            player.closeInventory();
            ExceptionReporter.reportException(e, player);
        }
    }

    private static final int CHECKOUT_WIDTH = 132;
    // The 131px sprite hangs below line 1's baseline (ascent 0), but the dialog only reserves
    // the body's text height: fill enough 9px lines to cover it, each carrying an invisible
    // full-width run so the whole texture stays clickable.
    private static final int CHECKOUT_LINES = 16;

    private static @NotNull Dialog buildCheckoutDialog(int productIndex, @NotNull String url) {
        var body = Component.text()
            .hoverEvent(HoverEvent.showText(LanguageProviderV2.translateMultiMerged("store.checkout.open_in_browser", List.of())))
            .clickEvent(ClickEvent.openUrl(url))
            .append(Component.text(SPRITE_MAP[productIndex].fontChar(), NamedTextColor.WHITE)
                .shadowColor(ShadowColor.none()))
            .append(Component.text(("\n" + FontUtil.computeOffset(CHECKOUT_WIDTH)).repeat(CHECKOUT_LINES - 1)))
            .build();

        return DialogBuilder.create()
            .title(Component.translatable("dialog.checkout.title"))
            .closeOnEscape()
            .body(it -> it.text(body, 150))
            .buildNotice(DialogButtons.close(Component.translatable("dialog.generic.close"), 150));
    }
}
