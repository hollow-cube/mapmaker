package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Pagination2;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PaginationElement<T extends View> extends ContainerElement implements Pagination2 {
    private final Class<T> itemClass;

    private List<Element> pageCache = new ArrayList<>();
    private Consumer<PageRequest<T>> pageHandler = null;
    private int maxPage = 0;
    private int page = 0;

    public PaginationElement(@NotNull ElementContext context, @Nullable String id, int width, int height, Class<T> itemClass) {
        super(context, id, width, height);
        this.itemClass = itemClass;
    }

    protected PaginationElement(@NotNull ElementContext context, @NotNull PaginationElement<T> other) {
        super(context, other);
        this.itemClass = other.itemClass;
    }

    @Override
    public void nextPage() {
        if (pageHandler == null) return;
        if (page >= maxPage) return;

        pageHandler.accept(new PageFetchRequest(this.page + 1));
    }

    @Override
    public void prevPage() {
        if (pageHandler == null) return;
        if (page <= 0) return;

        // Move back a page and trigger redraw
        page -= 1;
        context.markDirty();
    }

    @Override
    public void wireAction(@NotNull View view, @NotNull Method method) {
        method.setAccessible(true); // NOSONAR
        pageHandler = req -> {
            try {
                method.invoke(view, req);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public @NotNull PaginationElement<T> clone(@NotNull ElementContext context) {
        return new PaginationElement<>(context, this);
    }

    private class PageFetchRequest implements PageRequest<T> {
        private final int fetchPage;

        private PageFetchRequest(int fetchPage) {
            this.fetchPage = fetchPage;
        }

        @Override
        public @NotNull Context context() {
            return context;
        }

        @Override
        public int page() {
            return fetchPage;
        }

        @Override
        public int pageSize() {
            return PaginationElement.this.width() * PaginationElement.this.height();
        }

        @Override
        public void respond(@NotNull List<@NotNull T> view, boolean nextPage) {
            maxPage = nextPage ? Math.max(maxPage, fetchPage + 1) : fetchPage;
            page = fetchPage;

            // Build & cache the page for future use
            var pageContainer = new ContainerElement(
                    context, null,
                    PaginationElement.this.width(),
                    PaginationElement.this.height());
            for (var item : view) {
                pageContainer.addChild((BaseElement) item.element());
            }
            pageCache.add(fetchPage, pageContainer);

            // Redraw
            context.markDirty();
        }
    }
}
