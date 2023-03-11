package net.hollowcube.canvas;

public interface Pagination extends Element {

    void nextPage();

    void prevPage();

    interface PageRequest {
        int page();
        int width();
        int height();
//
//        /**
//         * Called to supply a response to the page request.
//         * <p>
//         * Implementations must be thread-safe.
//         */
//        void respond(@NotNull SectionLike view, boolean nextPage);
    }

}
