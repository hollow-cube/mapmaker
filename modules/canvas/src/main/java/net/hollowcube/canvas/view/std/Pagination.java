package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.State;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.canvas.view.ViewFunc;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Pagination {
    private Pagination() {}

    public record Page(@Nullable ViewFunc view, boolean hasNextPage) {}

    @FunctionalInterface
    public interface PageFn {
        @NotNull Page get(@NotNull ViewContext context, int page, int pageWidth, int pageHeight);
    }

    private static final State.Keyed<Integer, Page> pagesState = State.keyed("pages", page -> null);
    private static final State<Integer> currentPageState = State.value("currentPage", () -> 0);
    private static final State<@Nullable ViewFunc> currentPageViewState = State.derived("currentPageView", (get) -> {
        var page = get.get(pagesState, get.get(currentPageState));
        Check.notNull(page, "page is null"); // Should never happen, we load the first and future pages
        return page.view;
    });
    private static final State<@NotNull Boolean> hasNextPageState = State.derived("hasNextPage", (get) -> {
        var page = get.get(pagesState, get.get(currentPageState));
        Check.notNull(page, "page is null"); // Should never happen, we load the first and future pages
        return page.hasNextPage;
    });

    public static @NotNull Controller Controller(@NotNull ViewContext context, int pageWidth, int pageHeight, @NotNull PageFn pageFn, @NotNull ViewFunc emptyView) {
        return new Controller(context, pageWidth, pageHeight, pageFn, emptyView);
    }

    public record Controller(@NotNull ViewContext context, int width, int height, @NotNull PageFn pageFn, @NotNull ViewFunc emptyView) {

        public Controller {
            context.unsafeSet(pagesState, 0, pageFn.get(context, 0, width, height));
        }

        public @NotNull View PageView() {
            var page = context.get(currentPageState);
            var pageView = context.get(currentPageViewState);
            if (pageView == null)
                return context.create("empty", emptyView);
            return context.create("page" + page, pageView);
        }

        public @NotNull View NextPageButton(@NotNull ViewFunc view) {
            return context.create("nextPage", c -> View.Button(c, view, ClickHandler.leftClick(() -> {
                var hasNextPage = context.get(hasNextPageState);
                if (!hasNextPage) return;

                var page = context.get(currentPageState);
                var nextPage = context.get(pagesState, page + 1);
                if (nextPage == null) {
                    // Fetch the next page
                    context.set(pagesState, page + 1, pageFn.get(context, page + 1, width, height));
                }

                // Move to next page
                context.set(currentPageState, page + 1);
            })));
        }

        public @NotNull View PrevPageButton(@NotNull ViewFunc view) {
            return context.create("prevPage", c -> View.Button(c, view, ClickHandler.leftClick(() -> {
                var page = context.get(currentPageState);
                if (page == 0) return;

                var lastPage = context.get(pagesState, page - 1);
                if (lastPage == null) {
                    // Fetch the last page todo this is never the case right now
                    context.set(pagesState, page - 1, pageFn.get(context, page - 1, width, height));
                    return;
                }

                // Move to previous page
                context.set(currentPageState, page - 1);
            })));
        }
    }

}
