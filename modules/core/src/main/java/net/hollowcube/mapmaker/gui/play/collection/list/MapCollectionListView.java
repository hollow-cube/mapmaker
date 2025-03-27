package net.hollowcube.mapmaker.gui.play.collection.list;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapCollection;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MapCollectionListView extends View {

    private static final TextColor PAGE_COLOR = TextColor.color(0xBAC7D2);

    protected @ContextObject PlayerService playerService;
    protected @ContextObject MapService mapService;
    protected @ContextObject Player player;

    protected @Outlet("paging") Pagination pagination;
    protected @Outlet("title") Text title;
    protected @Outlet("page_number") Text pageNumber;

    private final String target;

    private List<MapCollection> collections;

    public MapCollectionListView(@NotNull Context context) {
        this(context, context.player().getUuid().toString());
    }

    public MapCollectionListView(@NotNull Context context, @NotNull String target) {
        super(context);

        this.target = target;
    }

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<MapCollectionEntry> request) {
        try {
            if (this.collections == null) {
                this.collections = mapService.getMapCollections(this.target);
                if (this.target.equals(request.context().player().getUuid().toString())) {
                    this.title.setText("Your Collections");
                } else {
                    var targetName = playerService.getPlayerDisplayName2(this.target).build(DisplayName.Context.PLAIN);
                    var component = Component.text()
                            .append(targetName)
                            .append(Component.text(" Collections"))
                            .build();
                    this.title.setText(component);
                }
            }

            var entries = new ArrayList<MapCollectionEntry>();
            for (var id : collections) {
                entries.add(new MapCollectionEntry(request.context(), id));
            }

            request.respond(entries, request.page() * request.pageSize() < this.collections.size());

            updatePageNumber(request.page());
        } catch (Exception e) {
            ExceptionReporter.reportException(e, this.player);
        }
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
