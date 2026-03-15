package net.hollowcube.mapmaker.api.notifications;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@RuntimeGson
public record Notification(
    String id,
    String key,
    String type,
    Instant createdAt,
    @Nullable Instant expiresAt,
    @Nullable Instant readAt,
    JsonObject data
) {
}
