package net.hollowcube.compat.lunar.payload;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PaginatedPayloadHandler {

    private final Cache<String, List<LunarPayload.Paginated<?>>> received = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(Duration.ofMinutes(1))
        .build();

    @SuppressWarnings("unchecked")
    public <T extends LunarPayload.Paginated<T>> @Nullable LunarPayload handle(LunarPayload.Paginated<T> payload) {
        var payloads = this.received.get(payload.id(), _ -> new ArrayList<>());
        payloads.add(payload);
        if (payload.isLastPage()) {
            this.received.invalidate(payload.id());

            return payload.group((List<T>) payloads);
        }
        return null;
    }
}
