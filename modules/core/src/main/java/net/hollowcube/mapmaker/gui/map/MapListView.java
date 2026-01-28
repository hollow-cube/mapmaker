package net.hollowcube.mapmaker.gui.map;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapHistory;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Pagination;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public abstract class MapListView extends Panel {
    protected final PlayerService playerService;
    protected final MapService mapService;
    protected final ServerBridge bridge;

    protected final Text titleText;
    private final Pagination<Unit> pagination;
    private boolean initialized = false;

    protected MapListView(
        @NotNull PlayerService playerService, @NotNull MapService mapService,
        @NotNull ServerBridge bridge, @NotNull String title
    ) {
        super(9, 10);
        this.playerService = playerService;
        this.mapService = mapService;
        this.bridge = bridge;

        background("generic2/containers/paginated/7x3", -10, -31);
        this.titleText = add(0, 0, title(title));

        add(0, 0, backOrClose());

        this.pagination = add(1, 1, new Pagination<Unit>(7, 3)
            .fetchAsync(this::onSearch));
        add(2, 4, pagination.prevButton());
        add(3, 4, pagination.pageText(3, 1));
        add(6, 4, pagination.nextButton());
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        if (!initialized) {
            initialized = true;
            pagination.reset(Unit.INSTANCE);
        }
    }

    @Blocking
    protected @NotNull List<? extends Panel> onSearch(@NotNull Unit ignored, int page, int pageSize) {
        var response = search(page, pageSize);
        if (response.getValue() > page) pagination.totalPages(response.getValue());

        var mapIds = new ArrayList<String>();
        var entries = new ArrayList<MapIconPanel>();
        for (var map : response.getKey()) {
            if (map.isCompletable()) mapIds.add(map.id());
            entries.add(new MapIconPanel(playerService, mapService, bridge, map));
        }

        // Fetch the player's current progress on the maps
        if (!mapIds.isEmpty()) async(() -> {
            var resp = mapService.getMapProgress(host.player().getUuid().toString(), mapIds);
            sync(() -> {
                for (var map : entries) {
                    var progress = resp.getProgress(map.map().id());
                    if (progress != null) map.updateProgress(progress);
                }
            });
        });

        return entries;
    }

    // Int is # of pages, only relevant if page == 0
    @Blocking
    protected abstract @NotNull Map.Entry<List<MapData>, Integer> search(int page, int pageSize);

    public static class Player extends MapListView {
        private final String targetId;

        public Player(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull ServerBridge bridge, @NotNull String targetId) {
            super(playerService, mapService, bridge, "Maps"); // Title is updated later.
            this.targetId = targetId;
        }

        @Override
        protected void mount(@NotNull InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);
            if (!isInitial) return;

            if (this.targetId.equals(host.player().getUuid().toString()))
                titleText.text("Your Maps");
            else {
                async(() -> {
                    var displayName = playerService.getPlayerDisplayName2(targetId);
                    sync(() -> titleText.text(displayName.getUsername() + "'s Maps"));
                });
            }
        }

        @Override
        protected Map.@NotNull Entry<List<MapData>, Integer> search(int page, int pageSize) {
            var response = mapService.searchMaps(MapSearchParams.builder(host.player().getUuid().toString())
                .page(page).pageSize(pageSize).owner(this.targetId).build());
            return Map.entry(response.results(), response.pageCount());
        }
    }

    public static class History extends MapListView {

        public History(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull ServerBridge bridge) {
            super(playerService, mapService, bridge, "Map History");
        }

        @Override
        protected Map.@NotNull Entry<List<MapData>, Integer> search(int page, int pageSize) {
            var playerId = PlayerData.fromPlayer(host.player()).id();
            var history = mapService.getPlayerMapHistory(playerId, page, pageSize);
            var mapIds = history.results().stream().map(MapHistory.Entry::mapId).toList();
            var maps = mapService.getMaps(playerId, mapIds);
            return Map.entry(maps, history.nextPage() ? page + 2 : page);
        }
    }
}
