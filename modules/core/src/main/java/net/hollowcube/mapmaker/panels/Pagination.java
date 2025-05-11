package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class Pagination<S> extends Panel {

    @FunctionalInterface
    public interface PageFetcher<S> {
        @NotNull List<? extends Panel> fetch(S search, int page, int pageSize);
    }

    private final List<IntConsumer> onPageChange = new ArrayList<>();
    private PageFetcher<S> pageFetcher;
    private PageFetcher<S> pageFetcherAsync;

    private S query = null;
    private int totalPages = 0;
    private int page = 0;

    public Pagination(int slotWidth, int slotHeight) {
        super(slotWidth, slotHeight);
    }

    // Imperative api

    public int totalPages() {
        return totalPages;
    }

    public void totalPages(int totalPages) {
        this.totalPages = totalPages;
        onPageChange.forEach(c -> c.accept(this.page));
    }

    public void reset() {
        // if on page 0 and not changing the query dont do anything.
        if (page == 0) return;

        resetSearch();
    }

    public void reset(@NotNull S search) {
        this.query = search;
        resetSearch();
    }

    public void prevPage() {
        if (page <= 0) return;
        page--;
        doPageFetch();
    }

    public void nextPage() {
        if (page >= totalPages - 1) return;
        page++;
        doPageFetch();
    }

    // DSL/builder

    public @NotNull Pagination<S> fetch(@NotNull PageFetcher<S> fetcher) {
        this.pageFetcher = fetcher;
        return this;
    }

    public @NotNull Pagination<S> fetchAsync(@NotNull PageFetcher<S> fetcher) {
        this.pageFetcherAsync = fetcher;
        return this;
    }

    public @NotNull Element prevButton() {
        var button = new Button("gui.generic.previous_page", 1, 1)
                .sprite("generic2/btn/page/prev", 5, 3);
        button.onLeftClick(_ -> prevPage());
        return button;
    }

    public @NotNull Element nextButton() {
        var button = new Button("gui.generic.next_page", 1, 1)
                .sprite("generic2/btn/page/next", 5, 3);
        button.onLeftClick(_ -> nextPage());
        return button;
    }

    public @NotNull Element pageText(int width, int height) {
        var button = new Text("", width, height, "")
                .align(Text.CENTER, 5);
        button.onLeftClick(_ -> reset());
        onPageChange.add(page -> {
            button.text((page + 1) + "/" + (totalPages == 0 ? "-" : totalPages));
            button.translationKey(totalPages == 0 ? "gui.generic.page" : "gui.generic.page_and_max", page + 1, totalPages);
        });
        return button;
    }

    // Impl

    private void resetSearch() {
        this.page = 0;
        this.totalPages = 0;
        doPageFetch();
    }

    private void doPageFetch() {
        if (this.pageFetcher != null) doPageFetch0(pageFetcher);
        else if (this.pageFetcherAsync != null) async(() -> doPageFetch0(pageFetcherAsync));
    }

    private void doPageFetch0(@NotNull PageFetcher<S> fetcher) {
        if (this.query == null) return;
        var results = fetcher.fetch(query, this.page, this.slotWidth * this.slotHeight);

        sync(() -> {
            clear();
            for (int i = 0; i < results.size(); i++) {
                var child = results.get(i);
                add(i % this.slotWidth, i / this.slotWidth, child);
            }
            onPageChange.forEach(c -> c.accept(this.page));
        });
    }
}
