package net.hollowcube.mapmaker.player.responses;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

@RuntimeGson
public record PlayerNotificationResponse(
    int page,
    int pageCount,
    List<ComplexEntry> results
) {

    @RuntimeGson
    public record ComplexEntry(
        @NotNull String id,

        @NotNull String type,
        @NotNull String key,

        @Nullable JsonObject data,

        @NotNull Instant createdAt,
        @Nullable Instant readAt,
        @Nullable Instant expiresAt
    ) {
    }

    @RuntimeGson
    public record SimpleEntry(
        @NotNull String playerId,
        @NotNull String action,
        @NotNull String type,
        @NotNull String key,
        @Nullable JsonObject data
    ) {
    }
}
