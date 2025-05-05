package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.gui.store.StoreHelpers.Package.HYPERCUBE_1MO;
import static net.hollowcube.mapmaker.gui.store.StoreHelpers.Package.HYPERCUBE_1Y;
import static net.hollowcube.mapmaker.gui.store.StoreHelpers.buyPackage;

class HypercubePanel extends Panel {

    public HypercubePanel(@NotNull PlayerService playerService) {
        super(9, 5);
        background("store/hypercube", 0, 1);

        // Feature callouts
        add(0, 0, button("gui.store.hypercube.perks.unlimited_map_slots", 2, 2));
        add(2, 1, button("gui.store.hypercube.perks.beta_testing", 2, 2));
        add(4, 1, button("gui.store.hypercube.perks.badge", 1, 2));
        add(5, 1, button("gui.store.hypercube.perks.more_emojis", 2, 2));
        add(7, 0, button("gui.store.hypercube.perks.all_map_sizes", 2, 2));

        // Purchase buttons
        add(1, 3, button("gui.store.hypercube.1month", 3, 2)
                .onLeftClickAsync(player -> buyPackage(playerService, player, HYPERCUBE_1MO)));
        add(5, 3, button("gui.store.hypercube.1year", 3, 2)
                .onLeftClickAsync(player -> buyPackage(playerService, player, HYPERCUBE_1Y)));
    }

}
