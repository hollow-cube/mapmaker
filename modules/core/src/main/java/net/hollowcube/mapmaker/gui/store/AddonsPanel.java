package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.hollowcube.mapmaker.gui.store.StoreHelpers.buyUpgrade;
import static net.hollowcube.mapmaker.gui.store.StoreHelpers.isUpgradeOwned;

class AddonsPanel extends Panel {
    private static final String[] COST_SPRITES = new String[]{
        "store/addons/cost_50_cubits",
        "store/addons/cost_100_cubits",
        "store/addons/cost_150_cubits",
    };

    private record Addon(ShopUpgrade id, String translation, int cost, String icon, int iconX, int iconY) {
    }

    private static final Addon[] MAP_SLOTS = new Addon[]{
        new Addon(ShopUpgrade.MAP_SLOT_3, "gui.store.addons.map_slots_3", 0, "store/addons/map_slot", 4, 4),
        new Addon(ShopUpgrade.MAP_SLOT_4, "gui.store.addons.map_slots_4", 1, "store/addons/map_slot", 4, 4),
        new Addon(ShopUpgrade.MAP_SLOT_5, "gui.store.addons.map_slots_5", 2, "store/addons/map_slot", 4, 4)
    };
    private static final Addon[] MAP_SIZES = new Addon[]{
        new Addon(ShopUpgrade.MAP_SIZE_2, "gui.store.addons.map_size_large", 0, "store/addons/map_size_2", 3, 4),
        new Addon(ShopUpgrade.MAP_SIZE_3, "gui.store.addons.map_size_massive", 1, "store/addons/map_size_3", 3, 3),
        new Addon(ShopUpgrade.MAP_SIZE_4, "gui.store.addons.map_size_colossal", 2, "store/addons/map_size_4", 2, 2)
    };
    private static final Addon[] BUILD_TOOLS = new Addon[]{
        new Addon(ShopUpgrade.BUILD_TOOLS, "gui.store.addons.terraform_advanced", 0, "store/addons/build_tools", 3, 2),
    };

    private final PlayerService playerService;

    public AddonsPanel(PlayerService playerService) {
        super(9, 5);
        this.playerService = playerService;

        background("store/addons/container", 0, 1);

        add(1, 1, new Entry(MAP_SLOTS));
        add(3, 1, new Entry(MAP_SIZES));
        add(5, 1, new Entry(BUILD_TOOLS));
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

        Entry(Addon[] chain) {
            super(1, 1);
            this.chain = chain;

            this.delegate = add(0, 0, button("", 1, 1)
                .onLeftClickAsync(this::handleBuyUpgrade));
            this.cost = add(0, 0, button("", 1, 0));
        }

        @Override
        protected void mount(InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);
            updateDisplay(host.player());
        }

        private void updateDisplay(Player player) {
            var firstLocked = firstLocked(player);
            Addon addon = Objects.requireNonNullElse(firstLocked, chain[chain.length - 1]);

            delegate.translationKey(addon.translation + (firstLocked != null ? "" : ".unlocked"));
            delegate.background(firstLocked == null ? "store/addons/slot_selected" : "store/addons/slot_default");
            delegate.sprite(addon.icon, addon.iconX, addon.iconY);

            if (firstLocked != null) {
                cost.background(COST_SPRITES[firstLocked.cost], -1, 24);
            } else {
                cost.background((Sprite) null);
            }
        }

        private void handleBuyUpgrade() {
            var firstLocked = firstLocked(host.player());
            if (firstLocked == null) return;

            buyUpgrade(playerService, host.player(), firstLocked.id);
            updateDisplay(host.player());
        }

        private @Nullable Addon firstLocked(Player player) {
            for (var addon : chain) {
                if (isUpgradeOwned(player, addon.id))
                    continue;
                return addon;
            }
            return null;
        }

    }

}
