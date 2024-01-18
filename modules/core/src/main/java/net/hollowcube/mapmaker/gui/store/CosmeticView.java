package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CosmeticView extends View {

    public CosmeticView(@NotNull Context context) {
        super(context);
    }

    @Action("cosmetic_list")
    private void fetchPage(@NotNull Pagination.PageRequest<CosmeticEntry> request) {
        var entries = new ArrayList<CosmeticEntry>();
        entries.add(new CosmeticEntry(request.context()));
        request.respond(entries, false);
    }

}
