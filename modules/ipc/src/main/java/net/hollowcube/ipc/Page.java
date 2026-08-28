package net.hollowcube.ipc;

/// One page of a paginated call, as the arguments a query wants rather than the ones a caller sent.
///
/// Pages are zero-based, and a page size that is missing or beyond what a caller should be asking
/// for becomes the default rather than an error — a client with a stale idea of the limit gets a
/// page, not a 400.
public record Page(long limit, long offset) {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    public static Page of(int page, int pageSize) {
        return of(page, pageSize, DEFAULT_SIZE, MAX_SIZE);
    }

    public static Page of(int page, int pageSize, int defaultSize, int maxSize) {
        long limit = pageSize <= 0 || pageSize > maxSize ? defaultSize : pageSize;
        return new Page(limit, page <= 0 ? 0 : page * limit);
    }
}
