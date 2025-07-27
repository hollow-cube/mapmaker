package net.hollowcube.mapmaker.editor.gui.buildermenu;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.editor.EditorMapWorld;

import java.util.ArrayList;
import java.util.List;

public class BuilderMenuTab extends View {

    private @Outlet("entries") Pagination entries;

    private final List<BuilderMenuTabItems.Item> items = new ArrayList<>();

    public BuilderMenuTab(Context context) {
        super(context);
    }

    @Action("entries")
    private void fetchEntries(Pagination.PageRequest<BuilderMenuEntry> request) {
        var player = request.context().player();
        var world = EditorMapWorld.forPlayer(player);

        var entries = new ArrayList<BuilderMenuEntry>();
        for (var item : items) {
            if (world == null || item.isVisible(world, player)) {
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
