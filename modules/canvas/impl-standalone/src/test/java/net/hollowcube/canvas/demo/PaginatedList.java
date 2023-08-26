package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

public class PaginatedList extends View {

    private @Outlet("pagination") Pagination pagination;

    public PaginatedList(@NotNull Context context) {
        super(context);
    }

    @Action("next")
    private void nextPage() {
        pagination.nextPage();
    }

    @Action("prev")
    private void prevPage() {
        pagination.prevPage();
    }

    @Action("pagination")
    private void fetchPage(@NotNull Pagination.PageRequest<PageItem> request) {
        var pageItems = IntStream.range(0, request.pageSize())
                .mapToObj(i -> {
                    var item = new PageItem(request.context());
                    item.setName("Item " + (request.page() * request.pageSize() + i + 1));
                    return item;
                })
                .toList();
        request.respond(pageItems, true);
    }

}
