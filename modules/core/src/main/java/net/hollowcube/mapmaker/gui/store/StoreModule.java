package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.hollowcube.mapmaker.scripting.util.Proxies;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.store.ShopUpgradeCache;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StoreModule {
    private static final Logger logger = LoggerFactory.getLogger(StoreModule.class);

    public static void openStoreView(
            @NotNull ScriptEngine scriptEngine, @NotNull PlayerService playerService,
            @NotNull PermManager permManager, @NotNull Player player, @Nullable String initialTab
    ) {
        try {
            scriptEngine.guiManager().openGui(player, URI.create("guilib:///store/store-view.js"),
                    Map.of("@mapmaker/internal/store", new StoreModule(playerService, permManager)),
                    OpUtils.build(new HashMap<>(), m -> m.put("initialTab", initialTab)));
        } catch (Exception e) {
            logger.error("failed to open store view", e);
            ExceptionReporter.reportException(e, player);
        }
    }

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
    private final PermManager permManager;

    public StoreModule(@NotNull PlayerService playerService, @NotNull PermManager permManager) {
        this.playerService = playerService;
        this.permManager = permManager;
    }

    @HostAccess.Export
    public void buyPackage(@NotNull String packageName) {
        final Player player = InventoryHost.current().player();
        FutureUtil.submitVirtual(() -> buyPackage0(player, packageName));
    }

    @HostAccess.Export
    public boolean isUpgradeOwned(@NotNull String upgradeId) {
        final Player player = InventoryHost.current().player();
        final ShopUpgrade upgrade = ShopUpgrade.BY_ID.get(upgradeId);
        if (upgrade == null) return false;
        return ShopUpgradeCache.has(player, upgrade, true);
    }

    @HostAccess.Export
    public Value buyUpgrade(@NotNull String upgradeId) {
        final Player player = InventoryHost.current().player();
        final ShopUpgrade upgrade = ShopUpgrade.BY_ID.get(upgradeId);
        if (upgrade == null) return Proxies.resolved(null);
        if (ShopUpgradeCache.has(player, upgrade, true))
            return Proxies.resolved(null); // Sanity check

        // Ensure the player has enough cubits to buy the upgrade.
        var playerData = PlayerDataV2.fromPlayer(player);
        var backpack = PlayerBackpack.fromPlayer(player);
        if (!upgrade.canAfford(playerData, backpack)) {
            // Cannot afford, prompt to buy more cubits

            //todo
            player.sendMessage(Component.translatable("currency.missing"));
            return Proxies.resolved(null);
        }

        return Proxies.async(() -> {
            buyUpgrade0(player, playerData, upgrade);
            return null;
        }, player.scheduler());
    }

    @Blocking
    private void buyPackage0(@NotNull Player player, @NotNull String packageName) {
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

    @Blocking
    private void buyUpgrade0(@NotNull Player player, @NotNull PlayerDataV2 playerData, @NotNull ShopUpgrade upgrade) {
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
