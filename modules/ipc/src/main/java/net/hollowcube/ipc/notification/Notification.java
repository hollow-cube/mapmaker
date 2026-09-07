package net.hollowcube.ipc.notification;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.ipc.util.JsonValueAdapter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record Notification(
    UUID id,
    String key,
    String type,
    Instant createdAt,
    @Nullable Instant expiresAt,
    @Nullable Instant readAt,
    @JsonAdapter(JsonValueAdapter.class) JsonObject data
) {}
