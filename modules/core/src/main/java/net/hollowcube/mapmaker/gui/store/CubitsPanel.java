package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.gui.store.StoreHelpers.Package.*;
import static net.hollowcube.mapmaker.gui.store.StoreHelpers.buyPackage;

class CubitsPanel extends Panel {

    public CubitsPanel(@NotNull PlayerService playerService) {
        super(9, 5);
        background("store/cubits", 0, 1);

        add(0, 0, button("gui.store.cubits.1", 3, 2)
                .onLeftClickAsync(() -> buyPackage(playerService, host.player(), CUBITS_50)));
        add(3, 0, button("gui.store.cubits.2", 3, 2)
                .onLeftClickAsync(() -> buyPackage(playerService, host.player(), CUBITS_105)));
        add(6, 0, button("gui.store.cubits.3", 3, 2)
                .onLeftClickAsync(() -> buyPackage(playerService, host.player(), CUBITS_220)));

        add(0, 2, button("gui.store.cubits.4", 4, 3)
                .onLeftClickAsync(() -> buyPackage(playerService, host.player(), CUBITS_400)));
        add(5, 2, button("gui.store.cubits.5", 4, 3)
                .onLeftClickAsync(() -> buyPackage(playerService, host.player(), CUBITS_600)));
    }

}
