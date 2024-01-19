package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PaginationElement<T extends View> extends BaseElement implements Pagination {
    private final Class<T> itemClass;
    private final boolean cached;

    private final List<BaseElement> pageCache = new ArrayList<>();
    private Consumer<PageRequest<T>> pageHandler = null;
    private int maxPage = 0;
    private int page = 0;

    public PaginationElement(@NotNull ElementContext context, @Nullable String id, int width, int height,
                             Class<T> itemClass, boolean cached) {
        super(context, id, width, height);
        this.itemClass = itemClass;
        this.cached = cached;
    }

    protected PaginationElement(@NotNull ElementContext context, @NotNull PaginationElement<T> other) {
        super(context, other);
        this.itemClass = other.itemClass;
        this.cached = other.cached;
    }

    @Override
    public boolean isAnyLoading() {
        if (super.isAnyLoading()) return true;
        if (page >= pageCache.size()) return true;
        return pageCache.get(page).isAnyLoading();
    }

    @Override
    public void reset() {
        page = 0;
        pageCache.clear();

        // Refetch first page
        if (pageHandler == null) return;
        //todo need some local "request id" to ensure an old request is not used multiple times
        pageHandler.accept(new PageFetchRequest(0));
    }

    @Override
    public void nextPage() {
        if (pageHandler == null) return;
        if (page >= maxPage) return;

        if (pageCache.size() > page + 1) {
            // Move forward a page and trigger redraw
            page += 1;
            performSignal(SIG_PAGE_CHANGED, page);
            context.markDirty();
        } else {
            // Fetch the next page from data source
            pageHandler.accept(new PageFetchRequest(this.page + 1));
        }
    }

    @Override
    public void prevPage() {
        if (pageHandler == null) return;
        if (page <= 0) return;

        // Move back a page and trigger redraw
        page -= 1;
        performSignal(SIG_PAGE_CHANGED, page);
        context.markDirty();
    }

    @Override
    public void wireAction(@NotNull View view, @NotNull Object handler, @NotNull Action.Descriptor action) {
        switch (handler) {
            case Method method -> {
                method.setAccessible(true); // NOSONAR

                Consumer<PageRequest<T>> handleFunc = req -> {
                    try {
                        method.invoke(view, req);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                pageHandler = !action.async() ? handleFunc :
                        req -> Thread.startVirtualThread(() -> {
                            setState(State.LOADING);
                            handleFunc.accept(req);
                        });
            }
            default -> throw new UnsupportedOperationException("Unsupported action handler: " + handler);
        }
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        if (SIG_MOUNT.equals(name)) mount();
        if (page >= pageCache.size()) return;
        pageCache.get(page).performSignal(name, args);
    }

    private void mount() {
        if (!cached || pageCache.isEmpty()) {
            reset();
        }
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        if (super.shouldDelegateDraw() || page >= pageCache.size())
            return super.getContents();
        return pageCache.get(page).getContents();
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (super.shouldIgnoreInput() || page >= pageCache.size())
            return CLICK_DENY;
        return pageCache.get(page).handleClick(player, slot, clickType);
    }

    @Override
    public @Nullable BaseElement findById(@NotNull String id) {
        var found = super.findById(id);
        if (found != null || page >= pageCache.size()) return found;
        return pageCache.get(page).findById(id);
    }

    @Override
    public void collectById(@NotNull Predicate<String> predicate, @NotNull List<Element> result) {
        super.collectById(predicate, result);
        if (page < pageCache.size())
            pageCache.get(page).collectById(predicate, result);
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
            context.player().scheduleNextTick(unused -> {
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
                setState(State.ACTIVE);
                performSignal(SIG_PAGE_CHANGED, page);
                context.markDirty();
            });
        }
    }
}
