package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapWorld;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BuilderMenuTab extends View {

    private @Outlet("entries") Pagination entries;

    private final List<BuilderMenuTabItems.Item> items = new ArrayList<>();

    public BuilderMenuTab(@NotNull Context context) {
        super(context);
    }

    @Action("entries")
    private void fetchEntries(@NotNull Pagination.PageRequest<BuilderMenuEntry> request) {
        var player = request.context().player();
        var map = MapWorld.forPlayerOptional(player);

        var entries = new ArrayList<BuilderMenuEntry>();
        for (var item : items) {
            if (map == null || item.isVisible(map, player)) {
                entries.add(new BuilderMenuEntry(request.context(), item));
            }
        }
        request.respond(entries, false);
    }

    public void setItems(BuilderMenuTabItems.Item... items) {
        this.items.clear();
        this.items.addAll(List.of(items));
        this.entries.reset();
    }
}
