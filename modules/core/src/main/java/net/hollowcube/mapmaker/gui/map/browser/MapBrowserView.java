package net.hollowcube.mapmaker.gui.map.browser;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.panels.Pagination;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class MapBrowserView extends Panel {

    private final PlayerService playerService;
    private final MapService mapService;

    private final Pagination pagination;

    public MapBrowserView(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super(9, 10);
        this.playerService = playerService;
        this.mapService = mapService;

        background("map_browser/container", -10, -31);
        add(0, 0, title("Play Maps"));

        add(0, 0, backOrClose());
        // todo search input

        this.pagination = add(1, 2, new Pagination(7, 3));
        add(2, 5, pagination.prevButton());
        add(3, 5, pagination.pageText(3, 1));
        add(6, 5, pagination.nextButton());

        add(0, 6, new SimpleSortPanel(this::onSearch));
    }

    private void onSearch(@NotNull MapSearchParams params) {
        System.out.println("reset search to: " + params);
    }

    @Override
    protected void unmount() {
        var playerData = PlayerDataV2.fromPlayer(host.player());
        FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(playerService));

        super.unmount();
    }
}
