package net.hollowcube.mapmaker.player.responses;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

@RuntimeGson
public record PlayerNotificationResponse(
    int page,
    int pageCount,
    List<ComplexEntry> results
) {

    public sealed interface Entry {
        String type();

        String key();

        @Nullable JsonObject data();
    }

    @RuntimeGson
    public record ComplexEntry(
        String id,

        String type,
        String key,

        @Nullable JsonObject data,

        Instant createdAt,
        @Nullable Instant readAt,
        @Nullable Instant expiresAt
    ) implements Entry {
    }

    @RuntimeGson
    public record SimpleEntry(
        String playerId,
        String action,
        String type,
        String key,
        @Nullable JsonObject data
    ) implements Entry {
    }
}
