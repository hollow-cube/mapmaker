package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class PaginatedList extends View {

    private @Outlet("pagination") Pagination pagination;

    @Action("next")
    private void nextPage() {
        pagination.nextPage();
    }

    @Action("prev")
    private void prevPage() {
        pagination.prevPage();
    }

    public PaginatedList(@NotNull Context context) {
        super(context);
    }

    @Action("pagination")
    private void fetchPage(@NotNull Pagination.PageRequest request) {
//        int pageSize = request.width() * request.height();
////        var view = IntStream.range(0, pageSize)
////                .mapToObj(i -> new PageItem("Item " + (request.page() * pageSize + i + 1)))
////                .collect(View.autoLayout(request.width(), request.height()));
//        var view = new BoxElement(null, request.width(), request.height(), BoxElement.Align.LTR);
//        IntStream.range(0, pageSize)
//                .mapToObj(i -> new PageItem("Item " + (request.page() * pageSize + i)))
//                .forEach(view::addChild);
////                .collect(View.autoLayout(request.width(), request.height()));
//        request.respond(view, true);
    }

}
