package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Switch;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class StoreView extends Panel {
    private static final List<String> TITLES = List.of("Buy Cubits", "Buy Hypercube", "Buy Add-ons");

    public static final int TAB_CUBITS = 0;
    public static final int TAB_HYPERCUBE = 1;
    public static final int TAB_ADDONS = 2;

    public StoreView(@NotNull PlayerService playerService, @NotNull PermManager permManager) {
        this(playerService, permManager, TAB_HYPERCUBE);
    }

    public StoreView(@NotNull PlayerService playerService, @NotNull PermManager permManager, int defaultTab) {
        super(9, 10);
        background("store/container", -10, -31);
        add(0, 0, title("Store"));

        add(0, 0, backOrClose());
        add(1, 0, info("store"));
        var title = add(2, 0, new Text("", 5, 1, TITLES.getFirst())
                .background("generic2/btn/default/5_1")
                .align(Text.CENTER, Text.CENTER));
        add(7, 0, button("gui.store.cubits_to_coins", 2, 1)
                .background("generic2/btn/default/2_1")
                .sprite("store/coins_to_cubits", 2, 3));

        var tabs = add(0, 1, new Switch(9, 5, List.of(
                new CubitsPanel(playerService),
                new HypercubePanel(playerService),
                new AddonsPanel(playerService, permManager)
        )));
        tabs.onSelect(index -> title.text(TITLES.get(index)));

        add(0, 6, tabs.button(TAB_CUBITS, 3, 3,
                "gui.store.tab_cubits", "store/cubits"));
        add(3, 6, tabs.button(TAB_HYPERCUBE, 3, 3,
                "gui.store.tab_hypercube", "store/ranks"));
        add(6, 6, tabs.button(TAB_ADDONS, 3, 3,
                "gui.store.tab_addons", "store/addons"));

        tabs.select(defaultTab);
    }

}
