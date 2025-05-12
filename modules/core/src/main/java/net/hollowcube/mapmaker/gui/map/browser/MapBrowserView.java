package net.hollowcube.mapmaker.gui.map.browser;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.StringComparison;
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
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class MapBrowserView extends Panel {

    private final PlayerService playerService;
    private final MapService mapService;

    private final Pagination<MapSearchParams.Builder> pagination;
    private final Text searchTextElement;

    private volatile String searchText = "";

    public MapBrowserView(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super(9, 10);
        this.playerService = playerService;
        this.mapService = mapService;

        background("map_browser/container", -10, -31);
        add(0, 0, title("Play Maps"));

        add(0, 0, backOrClose());
        this.searchTextElement = add(1, 0, new Text("gui.map_browser.search_maps.empty", 8, 1, "Search...")
                .align(8, 5));
        this.searchTextElement.onLeftClick(this::handleSearchBegin);
        this.searchTextElement.onShiftLeftClick(this::handleSearchClear);

        this.pagination = add(1, 2, new Pagination<MapSearchParams.Builder>(7, 3)
                .fetchAsync(this::onSearch));
        add(2, 5, pagination.prevButton());
        add(3, 5, pagination.pageText(3, 1));
        add(6, 5, pagination.nextButton());

        add(0, 6, new SimpleSortPanel(this::handleSortChange));
    }

    @Blocking
    private @NotNull List<? extends Panel> onSearch(@NotNull MapSearchParams.Builder params, int page, int pageSize) {
        // If we have a search query, ignore the given params.
        if (!this.searchText.isEmpty()) {
            params = MapSearchParams.builder(host.player().getUuid().toString())
                    .query(searchText);
        }

        var response = mapService.searchMaps(params.page(page).pageSize(pageSize).build());
        if (page == 0) pagination.totalPages(response.pageCount());

        // Sort the page of results using string similarity to the query
        var results = response.results();
        if (!this.searchText.isEmpty()) {
            results = new ArrayList<>(results);
            results.sort(StringComparison.jaroWinkler(searchText, MapData::name));
        }

        var mapIds = new ArrayList<String>();
        var entries = new ArrayList<MapEntry>();
        for (var map : results) {
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

    private void handleSortChange(@NotNull MapSearchParams.Builder params) {
        // Altering the sort params will reset any search query to use the search you input.
        handleSearchTextChange("");
        this.pagination.reset(params);
    }

    private void handleSearchBegin() {
        host.pushView(simpleAnvil(
                "generic2/anvil/field_container",
                "map_browser/search_anvil_icon",
                this::handleSearchTextChange
        ));
    }

    private void handleSearchClear() {
        handleSearchTextChange("");
    }

    private void handleSearchTextChange(@NotNull String newValue) {
        var oldValue = this.searchText;
        searchText = newValue.trim();
        if (searchText.equals(oldValue)) return;

        this.searchTextElement.translationKey(searchText.isEmpty() ? "gui.map_browser.search_maps.empty"
                : "gui.map_browser.search_maps", searchText);
        this.searchTextElement.text(searchText.isEmpty() ? "Search..." : searchText);
        this.pagination.reset();
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
        protected void mount(@NotNull InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);

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
