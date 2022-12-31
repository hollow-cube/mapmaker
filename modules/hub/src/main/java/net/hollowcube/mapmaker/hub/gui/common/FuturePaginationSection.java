package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pagination implementation using {@link net.hollowcube.common.result.FutureResult}s for fetching pages.
 * <p>
 * The first page is fetched on mount, so context is always available to the page fn.
 */
public class FuturePaginationSection extends ParentSection {
    private static final Logger logger = LoggerFactory.getLogger(FuturePaginationSection.class);

    private final List<Consumer<FuturePaginationSection>> updateHandlers = new ArrayList<>();

    private final PageFunction pageFn;
    private final List<Section> pages = new ArrayList<>();
    private int page = 0;
    private boolean hasNextPage = false;

    private long key = 0; // Updated on mount, used to handle cancellation of old requests.

    public FuturePaginationSection(int width, int height, @NotNull PageFunction pageFn) {
        super(width, height);
        this.pageFn = pageFn;
    }

    @Override
    protected void mount() {
        super.mount();

        // Enable loading
        add(0, 0, new ButtonSection(1, 1, ItemStack.of(Material.CLOCK).withDisplayName(Component.text("Loading..."))));

        // Fetch the first page (this is always valid because the state is reset on unmount)
        fetchPage(0);
    }

    @Override
    protected void unmount() {
        super.unmount();
        // Clear state on unmount
        reset();
    }

    private void pageLoaded(long key, @Nullable PageData pageData) {
        // If the key is different, this is an old request (eg new page, unmount, reset, etc)
        if (key != this.key) return;

        // Unmount the current page (or loading).
        clear();

        // If pageData is null it means we loaded the first page and it had no entries.
        // In this case we will show an empty page icon.
        // todo art for this
        if (pageData == null) {
            add(0, 0, new ButtonSection(width(), height(), ItemStack.of(Material.BARRIER).withDisplayName(Component.text("No entries"))));
            return;
        }

        // Add the page to the list of pages & mount it
        pages.add(page, pageData.section);
        hasNextPage = pageData.hasNextPage;
        mountChild(0, 0, pageData.section);
        this.key = 0; // Reset key

        notifyHandlers();
    }

    private void pageLoadFailed(long key, @NotNull Error error) {
        // Always log the error
        logger.error("failed to load page (key is {}}): {}", key == this.key ? "valid" : "invalid", error.message());

        // If the key is different, this is an old request (eg new page, unmount, reset, etc)
        if (key != this.key) return;

        throw new RuntimeException(error.message());
    }

    public void reset() {
        key = 0; // Reset key
        page = 0;
        pages.clear();
        clear(); // Unmount current page

        // Enable loading
        add(0, 0, new ButtonSection(1, 1, ItemStack.of(Material.CLOCK).withDisplayName(Component.text("Loading..."))));

        // Fetch the first page
        fetchPage(0);
        notifyHandlers();
    }

    public int getPage() {
        return page;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public boolean hasNextPage() {
        return page < pages.size() - 1 || hasNextPage;
    }

    public boolean isLoading() {
        return key != 0;
    }

    public void nextPage() {
        if (!hasNextPage() || isLoading()) return;

        // Unmount this page
        var thisPage = pages.get(page);
        unmountChild(0, 0, thisPage);

        page += 1;
        if (page < pages.size()) {
            // Page already loaded, we can just render it
            add(0, 0, pages.get(page));
        } else {
            // Mount the loading page
            add(0, 0, new ButtonSection(1, 1, ItemStack.of(Material.CLOCK).withDisplayName(Component.text("Loading..."))));

            // Fetch the next page
            fetchPage(page);
        }

        // Always notify on loading
        notifyHandlers();
    }

    public void previousPage() {
        // key != 0 ensures there is no request out right now
        if (!hasPreviousPage() || isLoading()) return;

        // Unmount this page
        var thisPage = pages.get(page);
        unmountChild(0, 0, thisPage);

        // Mount this page
        // Never null because of the hasPreviousPage check above
        var nextPage = pages.get(page - 1);
        mountChild(0, 0, nextPage);

        page -= 1;

        notifyHandlers();
    }

    private synchronized void fetchPage(int page) {
        // We create a new key value so we can compare it when finished loading to see if this request is still relevant.
        final var key = System.currentTimeMillis();
        this.key = key;
        pageFn.getPage(width(), height(), page)
                .then(pageData -> pageLoaded(key, pageData))
                .thenErr(err -> pageLoadFailed(key, err));
    }

    // Next/prev page buttons

    public @NotNull Section nextPageButton() {
        return new PageButton(1, 1, true);
    }

    //todo alternative function to either wrap another section in button functionality or take an icon fn like this
//    public @NotNull Section nextPageButton(@NotNull BiFunction<Boolean, Boolean, ItemStack> iconFn) {
//
//    }

    public @NotNull Section prevPageButton() {
        return new PageButton(1, 1, false);
    }

    // Change handlers

    public void addUpdateHandler(@NotNull Consumer<FuturePaginationSection> handler) {
        updateHandlers.add(handler);
    }

    public void removeUpdateHandler(@NotNull Consumer<FuturePaginationSection> handler) {
        updateHandlers.remove(handler);
    }

    private void notifyHandlers() {
        updateHandlers.forEach(handler -> handler.accept(this));
    }


    // Page function

    public record PageData(@NotNull Section section, boolean hasNextPage) {}

    @FunctionalInterface
    public interface PageFunction {
        @NotNull FutureResult<@Nullable PageData> getPage(int pageWidth, int pageHeight, int page);
    }


    // Page buttons

    private class PageButton extends ButtonSection implements Consumer<FuturePaginationSection> {
        private final boolean isNext;

        public PageButton(int width, int height, boolean next) {
            super(width, height, ItemStack.of(Material.ARROW),
                    next ? FuturePaginationSection.this::nextPage : FuturePaginationSection.this::previousPage);
            this.isNext = next;
            accept(FuturePaginationSection.this);
        }

        @Override
        protected void mount() {
            super.mount();
            addUpdateHandler(this);
        }

        @Override
        protected void unmount() {
            super.unmount();
            removeUpdateHandler(this);
        }

        @Override
        public void accept(FuturePaginationSection paginationComponent) {
            if (isNext ? hasNextPage() : hasPreviousPage()) {
                setItem(buildItem());
            } else {
                setItem(ItemStack.of(Material.AIR));
            }
        }

        private @NotNull ItemStack buildItem() {
            var name = Component.text(isNext ? "Next page" : "Previous page").decoration(TextDecoration.ITALIC, false);
            var lore = new ArrayList<Component>();
            if (isLoading()) {
                lore.add(Component.text("Loading...", NamedTextColor.GRAY, TextDecoration.ITALIC));
            }
            return ItemStack.of(Material.ARROW).withDisplayName(name).withLore(lore);
        }
    }

}
