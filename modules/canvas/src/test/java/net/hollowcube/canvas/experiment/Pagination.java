package net.hollowcube.canvas.experiment;

import org.jetbrains.annotations.NotNull;

public interface Pagination {

    void nextPage();

    void prevPage();

    interface PageRequest {
        int page();
        int width();
        int height();

        /**
         * Called to supply a response to the page request.
         * <p>
         * Implementations must be thread-safe.
         */
        void respond(@NotNull View view, boolean nextPage);
    }

}
