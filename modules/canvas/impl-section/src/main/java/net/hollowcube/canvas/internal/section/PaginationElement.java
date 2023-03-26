package net.hollowcube.canvas.internal.section;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.section.trait.DepthAware;
import net.hollowcube.canvas.section.SectionLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PaginationElement extends BaseParentElement implements Pagination, DepthAware {
    private Consumer<PageRequest> pageHandler = null;
    private List<SectionLike> pageCache = new ArrayList<>();

    private int maxPage = 0;
    private int page = 0;

    public PaginationElement(@Nullable String id, int width, int height) {
        super(id, width, height);
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

        // Replace the page with cached version
        clear();
        add(0, pageCache.get(--page));
    }

    @Override
    public void wireAction(@NotNull View view, @NotNull Method method) {
        method.setAccessible(true);
        pageHandler = req -> {
            try {
                method.invoke(view, req);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    protected void mount() {
        super.mount();

        // Reset
        clear();
        page = 0;
        pageCache.clear();

        if (pageHandler == null) return;

        //todo need some local "request id" to ensure an old request is not used multiple times
        pageHandler.accept(new PageFetchRequest(0));
    }

    private class PageFetchRequest implements PageRequest {
        private final int fetchPage;


        private PageFetchRequest(int fetchPage) {
            this.fetchPage = fetchPage;
        }


        @Override
        public int page() {
            return fetchPage;
        }

        @Override
        public int width() {
            return PaginationElement.this.width();
        }

        @Override
        public int height() {
            return PaginationElement.this.height();
        }

//        @Override
//        public void respond(@NotNull SectionLike view, boolean nextPage) {
//            //todo view should accept any element
//            maxPage = nextPage ? Math.max(maxPage, fetchPage + 1) : fetchPage;
//            page = fetchPage;
//
//            //todo need to check if the request is still valid
//
//            // Cache the page for future use
//            pageCache.add(fetchPage, view);
//
//            // Replace the page with the new version
//            clear();
//            add(0, view);
//        }
    }

    @Override
    public BaseElement clone() {
        return new PaginationElement(id(), width(), height());
    }
}
