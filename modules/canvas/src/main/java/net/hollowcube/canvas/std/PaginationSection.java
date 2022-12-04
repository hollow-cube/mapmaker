package net.hollowcube.canvas.std;

import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.ParentSection;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class PaginationSection extends ParentSection {
    private final List<Consumer<PaginationSection>> updateHandlers = new ArrayList<>();

    private final Function<Integer, @Nullable Section> pageFn;
    private final Map<Integer, Section> pages = new HashMap<>(); //todo should be list i guess
    private int page = 0;

    public PaginationSection(int width, int height, @NotNull Function<Integer, @Nullable Section> pageFn) {
        super(width, height);
        this.pageFn = pageFn;

        var firstPage = pageFn.apply(0);
        if (firstPage != null) {
            add(0, 0, firstPage);
            pages.put(0, firstPage);
            pages.put(1, pageFn.apply(1));
        } else {
            add(0, 0, new EmptySection(width, height));
        }
    }

    public int getPage() {
        return page;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public boolean hasNextPage() {
        return pages.getOrDefault(page + 1, null) != null;
    }

    public void nextPage() {
        if (!hasNextPage()) return;

        // Unmount this page
        var thisPage = pages.get(page);
        unmountChild(0, 0, thisPage);

        // Mount this page
        // Never null because of the hasNextPage check above
        var nextPage = pages.get(page + 1);
        mountChild(0, 0, nextPage);

        page += 1;

        // Fetch next page
        //todo should only do this fetch when it hasnt been fetched before
        //todo although some impls may not want to cache.
        pages.put(page + 1, pageFn.apply(page + 1));

        notifyHandlers();
    }

    public void previousPage() {
        if (!hasPreviousPage()) return;

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

    public @NotNull Section nextPageButton(int width, int height) {
        return new PageButton(width, height, true);
    }

    public @NotNull Section lastPageButton(int width, int height) {
        return new PageButton(width, height, false);
    }

    // Change handlers

    public void addUpdateHandler(@NotNull Consumer<PaginationSection> handler) {
        updateHandlers.add(handler);
    }

    public void removeUpdateHandler(@NotNull Consumer<PaginationSection> handler) {
        updateHandlers.remove(handler);
    }

    private void notifyHandlers() {
        updateHandlers.forEach(handler -> handler.accept(this));
    }


    // Page buttons

    private class PageButton extends ButtonSection implements Consumer<PaginationSection> {
        private boolean isNext;

        public PageButton(int width, int height, boolean next) {
            super(width, height, ItemStack.of(Material.ARROW),
                    next ? PaginationSection.this::nextPage : PaginationSection.this::previousPage);
            this.isNext = next;
            accept(PaginationSection.this);
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
        public void accept(PaginationSection paginationComponent) {
            if (isNext ? hasNextPage() : hasPreviousPage()) {
                setItem(ItemStack.of(Material.ARROW));
            } else {
                setItem(ItemStack.of(Material.AIR));
            }
        }
    }

}
