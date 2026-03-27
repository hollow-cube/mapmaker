package net.hollowcube.mapmaker.api.notifications;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.PaginatedList;

import java.util.Map;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;
import static net.hollowcube.mapmaker.api.HttpClientWrapper.query;

public interface NotificationClient {

    default PaginatedList<Notification> list(String playerId, int page, int pageSize, boolean unreadOnly) {
        throw notImplemented();
    }

    default void setReadStatus(String notificationId, boolean read) {
        throw notImplemented();
    }

    default void delete(String notificationId) {
        throw notImplemented();
    }

    record Noop() implements NotificationClient {}

    record Http(HttpClientWrapper http) implements NotificationClient {
        private static final String V4_PREFIX = "/v4/internal/notifications";

        @Override
        public PaginatedList<Notification> list(String playerId, int page, int pageSize, boolean unreadOnly) {
            return http.get(
                "notifications.list",
                V4_PREFIX + query("playerId", playerId, "page", page, "pageSize", pageSize, "unreadOnly", unreadOnly),
                new TypeToken<>() {});
        }

        @Override
        public void setReadStatus(String notificationId, boolean read) {
            http.patch(
                "notifications.setReadStatus",
                V4_PREFIX + "/" + notificationId,
                Map.of("read", read));
        }

        @Override
        public void delete(String notificationId) {
            http.delete(
                "notifications.delete",
                V4_PREFIX + "/" + notificationId);
        }
    }
}
