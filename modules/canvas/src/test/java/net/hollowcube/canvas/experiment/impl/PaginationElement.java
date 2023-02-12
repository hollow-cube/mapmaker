package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.SectionLike;
import net.hollowcube.canvas.experiment.Pagination;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PaginationElement extends ParentSection implements Element, Pagination {

    private final String id;

    private Consumer<PageRequest> pageHandler = null;

    private List<SectionLike> pageCache = new ArrayList<>();

    private int page = 0;
    private int maxPage = 0;

    public PaginationElement(@Nullable String id, int width, int height) {
        super(width, height);
        this.id = id;
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    public void setPageHandler(@NotNull Consumer<PageRequest> pageHandler) {
        this.pageHandler = pageHandler;
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

    // Impl


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

        @Override
        public void respond(@NotNull SectionLike view, boolean nextPage) {
            //todo view should accept any element
            maxPage = nextPage ? Math.max(maxPage, fetchPage + 1) : fetchPage;
            page = fetchPage;

            //todo need to check if the request is still valid

            // Cache the page for future use
            pageCache.add(fetchPage, view);

            // Replace the page with the new version
            clear();
            add(0, view);
        }
    }
}
