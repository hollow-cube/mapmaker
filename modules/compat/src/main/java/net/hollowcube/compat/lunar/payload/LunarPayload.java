package net.hollowcube.compat.lunar.payload;

import com.google.gson.JsonElement;

import java.util.List;

public interface LunarPayload {

    interface Paginated<T extends Paginated<T>> extends LunarPayload {
        String id();
        int page();
        int totalPages();

        LunarPayload group(List<T> pages);

        default boolean isLastPage() {
            return this.page() >= this.totalPages() - 1;
        }
    }

    record Unhandled(JsonElement json) implements LunarPayload {}
}
