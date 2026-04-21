package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.confirm;
import static net.hollowcube.mapmaker.gui.store.StoreHelpers.buyUpgrade;
import static net.hollowcube.mapmaker.gui.store.StoreHelpers.isUpgradeOwned;

class AddonsPanel extends Panel {
    private static final String[] COST_SPRITES = new String[]{
        "store/addons/cost_50_cubits",
        "store/addons/cost_100_cubits",
        "store/addons/cost_150_cubits",
    };

    private record Addon(ShopUpgrade id, String translation, int cost, String icon) {
    }

    private static final Addon[] MAP_SLOTS = new Addon[]{
        new Addon(ShopUpgrade.MAP_SLOT, "gui.store.addons.map_slot", 0, "sd_card"),
    };
    private static final Addon[] MAP_SIZES = new Addon[]{
        new Addon(ShopUpgrade.MAP_SIZE_2, "gui.store.addons.map_size_large", 0, "house_2"),
        new Addon(ShopUpgrade.MAP_SIZE_3, "gui.store.addons.map_size_massive", 1, "house_3"),
        new Addon(ShopUpgrade.MAP_SIZE_4, "gui.store.addons.map_size_colossal", 2, "castle")
    };
    private static final Addon[] MAP_BUILDERS = new Addon[]{
        new Addon(ShopUpgrade.MAP_BUILDER_2, "gui.store.addons.map_builder", 0, "sd_card"), // TODO: icon
        new Addon(ShopUpgrade.MAP_BUILDER_3, "gui.store.addons.map_builder", 0, "sd_card"),
        new Addon(ShopUpgrade.MAP_BUILDER_4, "gui.store.addons.map_builder", 0, "sd_card")
    };

    private final PlayerService playerService;

    public AddonsPanel(@NotNull PlayerService playerService) {
        super(9, 5);
        this.playerService = playerService;

        background("store/addons/container", 0, 1);

        add(1, 1, new Entry(MAP_SLOTS));
        add(3, 1, new Entry(MAP_SIZES));
        add(5, 1, new Entry(MAP_BUILDERS));
        add(7, 1, button("", 1, 1)
            .sprite("store/addons/slot_default"));

        add(1, 3, button("", 1, 1)
            .sprite("store/addons/slot_default"));
        add(3, 3, button("", 1, 1)
            .sprite("store/addons/slot_default"));
        add(5, 3, button("", 1, 1)
            .sprite("store/addons/slot_default"));
        add(7, 3, button("", 1, 1)
            .sprite("store/addons/slot_default"));

    }

    private class Entry extends Panel {
        private final Addon[] chain;

        private final Button delegate;
        private final Button cost;

        Entry(@NotNull Addon[] chain) {
            super(1, 1);
            this.chain = chain;

            this.delegate = add(0, 0, button("", 1, 1)
                .onLeftClick(this::handlePreBuyUpgrade));
            this.cost = add(0, 0, button("", 1, 0));
        }

        @Override
        protected void mount(@NotNull InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);
            updateDisplay(host.player());
        }

        private void updateDisplay(@NotNull Player player) {
            var firstLocked = firstLocked(player);
            Addon addon = Objects.requireNonNullElse(firstLocked, chain[chain.length - 1]);

            delegate.translationKey(addon.translation + (firstLocked != null ? "" : ".unlocked"), countComponent());
            delegate.background(firstLocked == null ? "store/addons/slot_selected" : "store/addons/slot_default");
            delegate.sprite("icon2/1_1/" + addon.icon, 1, 1);

            if (firstLocked != null) {
                cost.background(COST_SPRITES[firstLocked.cost], -1, 24);
            } else {
                cost.background((Sprite) null);
            }
        }

        private void handlePreBuyUpgrade() {
            var firstLocked = firstLocked(host.player());
            Addon addon = Objects.requireNonNullElse(firstLocked, chain[chain.length - 1]);

            var name = LanguageProviderV2.translateToPlain(Component.translatable(addon.translation + ".name", countComponent()));
            host.pushView(confirm("Buy " + name + "?", FutureUtil.wrapVirtual(this::handleBuyUpgrade)));
        }

        private void handleBuyUpgrade() {
            var firstLocked = firstLocked(host.player());
            if (firstLocked == null) return;

            buyUpgrade(playerService, host.player(), firstLocked.id);
            updateDisplay(host.player());
        }

        private @Nullable Addon firstLocked(@NotNull Player player) {
            for (var addon : chain) {
                if (isUpgradeOwned(player, addon.id))
                    continue;
                return addon;
            }
            return null;
        }

        private Component countComponent() {
            var playerData = PlayerData.fromPlayer(host.player());
            return Component.text(switch (chain[0].id) {
                case MAP_SLOT -> {
                    int totalSlots = playerData.mapSlots();
                    if (playerData.has(Permission.EXTENDED_LIMITS))
                        totalSlots -= 3;
                    yield String.valueOf(totalSlots + 1);
                }
                case MAP_BUILDER_2, MAP_BUILDER_3, MAP_BUILDER_4 -> {
                    int totalBuilders = playerData.mapBuilders();
                    yield String.valueOf(totalBuilders + 1);
                }
                default -> "";
            });
        }

    }

}
