package net.hollowcube.mapmaker.gui.map;

import net.hollowcube.ipc.map.MapData;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.ipc.map.MapSearch;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Pagination;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;
import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public abstract class MapListView extends Panel {
    protected final ApiClient api;
    protected final ServerBridge bridge;

    protected final Text titleText;
    private final Pagination<Unit> pagination;
    private boolean initialized = false;

    protected MapListView(@NotNull ApiClient api, @NotNull ServerBridge bridge, @NotNull String title) {
        super(9, 10);
        this.api = api;
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

        var playerId = host.player().getUuid();
        var mapIds = new ArrayList<UUID>();
        var entries = new ArrayList<MapIconPanel>();
        for (var map : response.getKey()) {
            if (map.isCompletable()) mapIds.add(map.id());
            entries.add(new MapIconPanel(api, bridge, map));
        }

        // Fetch the player's current progress on the maps
        if (!mapIds.isEmpty()) async(() -> {
            var progress = api.mapService.progress(playerId, mapIds);
            sync(() -> {
                for (var entry : progress) {
                    entries.stream().filter(map -> map.map().id().equals(entry.mapId()))
                        .forEach(map -> map.updateProgress(entry));
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

        public Player(@NotNull ApiClient api, @NotNull ServerBridge bridge, @NotNull String targetId) {
            super(api, bridge, "Maps"); // Title is updated later.
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
                    var displayName = api.players.displayName(UUID.fromString(targetId));
                    sync(() -> titleText.text(displayName.username() + "'s Maps"));
                });
            }
        }

        @Override
        protected Map.@NotNull Entry<List<MapData>, Integer> search(int page, int pageSize) {
            var response = api.mapService.search(MapSearch.builder()
                .page(page).pageSize(pageSize).owner(UUID.fromString(this.targetId)).build());
            return Map.entry(response.results(), response.totalPages(pageSize));
        }
    }

    public static class History extends MapListView {

        public History(@NotNull ApiClient api, @NotNull ServerBridge bridge) {
            super(api, bridge, "Map History");
        }

        @Override
        protected Map.@NotNull Entry<List<MapData>, Integer> search(int page, int pageSize) {
            var history = api.mapService.history(localPlayer(host.player()).id(), page, pageSize);
            return Map.entry(history.results(), history.totalPages(pageSize));
        }
    }
}
