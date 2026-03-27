package net.hollowcube.mapmaker.api;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

@RuntimeGson
public record PaginatedList<T>(int count, List<T> results) {

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
