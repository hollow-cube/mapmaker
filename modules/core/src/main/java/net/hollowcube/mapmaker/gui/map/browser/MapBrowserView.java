package net.hollowcube.mapmaker.gui.map.browser;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Pagination;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class MapBrowserView extends Panel {

    private final PlayerService playerService;
    private final MapService mapService;

    private final Pagination<MapSearchParams.Builder> pagination;

    public MapBrowserView(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super(9, 10);
        this.playerService = playerService;
        this.mapService = mapService;

        background("map_browser/container", -10, -31);
        add(0, 0, title("Play Maps"));

        add(0, 0, backOrClose());
        // todo search input

        this.pagination = add(1, 2, new Pagination<MapSearchParams.Builder>(7, 3)
                .fetchAsync(this::onSearch));
        add(2, 5, pagination.prevButton());
        add(3, 5, pagination.pageText(3, 1));
        add(6, 5, pagination.nextButton());

        add(0, 6, new SimpleSortPanel(pagination::reset));
    }

    @Blocking
    private @NotNull List<? extends Panel> onSearch(@NotNull MapSearchParams.Builder params, int page, int pageSize) {
        var response = mapService.searchMaps(params.page(page).pageSize(pageSize).build());
        if (page == 0) pagination.totalPages(response.pageCount());

        var mapIds = new ArrayList<String>();
        var entries = new ArrayList<MapEntry>();
        for (var map : response.results()) {
            if (map.isCompletable()) mapIds.add(map.id());
            entries.add(new MapEntry(map));
        }

        // Fetch the player's current progress on the maps
        if (!mapIds.isEmpty()) async(() -> {
            var resp = mapService.getMapProgress(host.player().getUuid().toString(), mapIds);
            sync(() -> {
                for (var map : entries) {
                    var progress = resp.getProgress(map.map.id());
                    if (progress != null) map.updateProgress(progress);
                }
            });
        });

        return entries;
    }

    @Override
    protected void unmount() {
        var playerData = PlayerDataV2.fromPlayer(host.player());
        FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(playerService));

        super.unmount();
    }

    private class MapEntry extends Panel {
        private final MapData map;
        private DisplayName authorName = null; // Filled async
        private Map.Entry<PersonalizedMapData.Progress, Integer> progress = null; // Filled async

        private final Button button;

        public MapEntry(@NotNull MapData map) {
            super(1, 1);
            this.map = map;

            this.button = add(0, 0, new Button(null, 1, 1));
        }

        @Override
        protected void mount(@NotNull InventoryHost host) {
            super.mount(host);

            if (this.authorName != null) return;
            async(() -> updateAuthor(playerService.getPlayerDisplayName2(map.owner())));
        }

        private void updateAuthor(@NotNull DisplayName displayName) {
            sync(() -> {
                authorName = displayName;
                updateIcon();
            });
        }

        private void updateProgress(@NotNull Map.Entry<PersonalizedMapData.Progress, Integer> progress) {
            sync(() -> {
                this.progress = progress;
                updateIcon();
            });
        }

        private void updateIcon() {
            var icon = Objects.requireNonNullElse(map.settings().getIcon(), Material.PAPER);
            button.model(icon.name(), null);

            var authorName = OpUtils.mapOr(this.authorName, DisplayName::build, Component.text("loading"));
            var entry = MapData.createHoverComponents(map, authorName, progress);
            entry.getValue().addAll(LanguageProviderV2.translateMulti("gui.play_maps.map_display.footer", List.of()));
            button.text(entry.getKey(), entry.getValue());
        }
    }
}
