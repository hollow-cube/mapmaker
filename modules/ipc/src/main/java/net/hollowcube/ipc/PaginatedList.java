package net.hollowcube.ipc;

import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

public record PaginatedList<T>(int count, List<T> results) {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    /// The `limit` a query wants for a page size a caller sent: one that is missing or beyond what a
    /// caller should be asking for becomes the default rather than an error, so a client with a
    /// stale idea of the maximum still gets a page.
    public static long limit(int pageSize) {
        return limit(pageSize, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    public static long limit(int pageSize, int defaultSize, int maxSize) {
        return pageSize <= 0 || pageSize > maxSize ? defaultSize : pageSize;
    }

    /// Pages are zero-based.
    public static long offset(int page, int pageSize) {
        return page <= 0 ? 0 : page * limit(pageSize);
    }

    /// One page of query rows, where the total is a `count(*) over ()` window that every row of the
    /// page carries a copy of. No rows means no matches, which is the one case the window cannot
    /// report because there is nothing to have read it off.
    public static <R, T> PaginatedList<T> of(
        List<R> rows,
        ToLongFunction<R> total,
        Function<R, T> item
    ) {
        if (rows.isEmpty()) return new PaginatedList<>(0, List.of());
        return new PaginatedList<>(
            (int) total.applyAsLong(rows.getFirst()),
            rows.stream().map(item).toList()
        );
    }

    public int totalPages(int pageSize) {
        return (int) Math.ceil((double) count / pageSize);
    }

    public boolean hasNext(int page, int pageSize) {
        return count > (page + 1) * pageSize;
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public @UnknownNullability T first() {
        return results.getFirst();
    }
}
