package net.hollowcube.mapmaker.gui.play.collection;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.play.ProgressMapEntry;
import net.hollowcube.mapmaker.map.MapCollection;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapProgressBatchResponse;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseMapCollectionView<T extends View & ProgressMapEntry> extends View {

    private static final TextColor PAGE_COLOR = TextColor.color(0xBAC7D2);

    protected @ContextObject PlayerService playerService;
    protected @ContextObject MapService mapService;
    protected @ContextObject Player player;

    protected @Outlet("paging") Pagination pagination;
    private @Outlet("page_number") Text pageNumber;

    private final Map<String, MapData> mapCache = new ConcurrentHashMap<>();
    private final Map<String, MapProgressBatchResponse.Entry> progressCache = new ConcurrentHashMap<>();

    protected final String id;
    protected MapCollection collection;
    protected Component ownerName;

    public BaseMapCollectionView(@NotNull Context context, @NotNull MapCollection collection) {
        this(context, collection.collectionId());

        this.collection = collection;
        this.ownerName = playerService.getPlayerDisplayName2(collection.owner()).asComponent();
        this.onLoaded(collection);
    }

    public BaseMapCollectionView(@NotNull Context context, @NotNull String id) {
        super(context);

        this.id = id;
    }

    protected void onLoaded(@NotNull MapCollection collection) {

    }

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<T> request) {
        try {
            if (this.collection == null) {
                this.collection = mapService.getMapCollection(this.player.getUuid().toString(), this.id);
                this.ownerName = playerService.getPlayerDisplayName2(this.collection.owner()).asComponent();
                this.onLoaded(this.collection);
            }

            var mapIds = this.collection.mapIds().subList(
                    Math.min(request.page() * request.pageSize(), this.collection.mapIds().size()),
                    Math.min(request.page() * request.pageSize() + request.pageSize(), this.collection.mapIds().size())
            );

            var mapsToFetch = new HashSet<>(mapIds);
            mapsToFetch.removeAll(this.mapCache.keySet());

            if (!mapsToFetch.isEmpty()) {
                for (MapData map : mapService.getMaps(this.player.getUuid().toString(), mapsToFetch.stream().toList())) {
                    this.mapCache.put(map.id(), map);
                }
            }

            var entries = new ArrayList<T>();
            for (var id : mapIds) {
                var data = this.mapCache.get(id);
                if (data == null) continue;

                entries.add(createEntry(request.context(), data));
            }

            request.respond(entries, request.page() * request.pageSize() + request.pageSize() < this.collection.mapIds().size());

            // Fetch the player's current progress on the maps
            var idsToFetch = new HashSet<>(mapIds);
            idsToFetch.removeAll(progressCache.keySet());

            if (idsToFetch.isEmpty()) return;
            final int page = request.page();
            async(() -> {
                var resp = mapService.getMapProgress(player.getUuid().toString(), idsToFetch.stream().toList());
                resp.progress().forEach(e -> progressCache.put(e.mapId(), e));

                player.scheduleNextTick(ignored -> {
                    if (page != pagination.page()) return;
                    pagination.<T>forEachEntry(page, entry -> {
                        var progress = progressCache.get(entry.map().id());
                        if (progress != null) entry.setProgress(progress.progress(), progress.playtime());
                    });
                });
            });

            updatePageNumber(page);
        } catch (Exception e) {
            ExceptionReporter.reportException(e, this.player);
        }
    }

    protected abstract T createEntry(@NotNull Context context, @NotNull MapData data);

    @Action("info")
    private void info() {
        if (this.collection == null) return;

        player.closeInventory();

        Component authorName;
        try {
            authorName = playerService.getPlayerDisplayName2(this.collection.owner()).build(DisplayName.Context.DEFAULT);
        } catch (Throwable t) {
            ExceptionReporter.reportException(t, player);
            authorName = Component.text("Unknown", NamedTextColor.RED);
        }

        player.sendMessage(LanguageProviderV2.translateMultiMerged("chat.map_collection.info.id", List.of(
                Component.text(collection.collectionId()),
                OpUtils.mapOr(collection.name(), Component::text, Component.text("Unnamed")),
                authorName
        )));
    }

    @Action("back")
    private void back() {
        pagination.prevPage();
        updatePageNumber(pagination.page());
    }

    @Action("next")
    private void next() {
        pagination.nextPage();
        updatePageNumber(pagination.page());
    }

    private void updatePageNumber(int page) {
        pageNumber.setArgs(Component.text(page + 1).color(PAGE_COLOR));
        pageNumber.setText(String.valueOf(page + 1), PAGE_COLOR);
    }
}
