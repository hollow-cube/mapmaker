package net.hollowcube.ipc.notification;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.ipc.util.JsonValueAdapter;
import net.hollowcube.ipc.util.NatsMessage;
import net.hollowcube.ipc.util.Publishable;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/// Matches Go notification.Manager messages during the shared-database rollout.
@NatsMessage(subject = "notification.>")
public record NotificationUpdate(
    String action,
    UUID playerId,
    String type,
    String key,
    @JsonAdapter(JsonValueAdapter.class) @Nullable JsonObject data
) implements Publishable {
    public static final String CREATE = "create";
    public static final String DELETE = "delete";

    @Override
    public String subject() {
        return switch (action) {
            case CREATE -> "notification.created";
            case DELETE -> "notification.deleted";
            default -> throw new IllegalArgumentException("not a notification action: " + action);
        };
    }
}
