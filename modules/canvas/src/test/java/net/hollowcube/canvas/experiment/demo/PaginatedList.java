package net.hollowcube.canvas.experiment.demo;

import net.hollowcube.canvas.experiment.Pagination;
import net.hollowcube.canvas.experiment.View;
import net.hollowcube.canvas.experiment.annotation.Action;
import net.hollowcube.canvas.experiment.annotation.Outlet;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

public class PaginatedList extends View {

    private @Outlet("pagination") Pagination pagination;

    @Action("abc")
    private void fetchPage(@NotNull Pagination.PageRequest request) {
        int pageSize = request.width() * request.height();
        var view = IntStream.range(0, pageSize)
                .mapToObj(i -> new PageItem("Item " + (request.page() * pageSize + i + 1)))
                .collect(View.autoLayout(request.width(), request.height()));
        request.respond(view, true);
    }

}
