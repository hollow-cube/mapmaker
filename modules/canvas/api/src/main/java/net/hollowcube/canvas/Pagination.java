package net.hollowcube.canvas;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public interface Pagination extends Element {

    String SIG_PAGE_CHANGED = "pagination/page_changed";

    int page();

    <T extends View> void forEachEntry(int page, @NotNull Consumer<T> consumer);

    void reset();

    void nextPage();

    void prevPage();

    interface PageRequest<T extends View> {
        @NotNull Context context();

        int page();

        int pageSize();

        /**
         * Called to supply a response to the page request.
         * <p>
         * Implementations must be thread-safe.
         */
        void respond(@NotNull List<@NotNull T> view, boolean nextPage);
    }

}
