package net.hollowcube.ipc;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

@RuntimeGson
public record PaginatedList<T>(int count, List<T> results) {

    /// One page of query rows, where the total is a `count(*) over ()` window that every row of the
    /// page carries a copy of. No rows means no matches, which is the one case the window cannot
    /// report because there is nothing to have read it off.
    public static <R, T> PaginatedList<T> of(List<R> rows, ToLongFunction<R> total, Function<R, T> item) {
        if (rows.isEmpty()) return new PaginatedList<>(0, List.of());
        return new PaginatedList<>((int) total.applyAsLong(rows.getFirst()), rows.stream().map(item).toList());
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
