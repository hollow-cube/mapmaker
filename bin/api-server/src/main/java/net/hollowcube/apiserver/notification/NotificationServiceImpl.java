package net.hollowcube.apiserver.notification;

import com.google.gson.JsonObject;
import net.hollowcube.apiserver.common.Json;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.NotificationsQueries;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.notification.Notification;
import net.hollowcube.ipc.notification.NotificationService;
import net.hollowcube.ipc.notification.NotificationUpdate;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

import static net.hollowcube.ipc.PaginatedList.limit;
import static net.hollowcube.ipc.PaginatedList.offset;

public final class NotificationServiceImpl implements NotificationService {

    private final ApiDatabase db;
    private final NatsPublisher nats;

    public NotificationServiceImpl(ApiDatabase db, NatsPublisher nats) {
        this.db = db;
        this.nats = nats;
    }

    @Override
    public PaginatedList<Notification> list(
        UUID playerId,
        boolean unreadOnly,
        int page,
        int pageSize
    ) {
        var rows = db.notifications.list(
            playerId,
            unreadOnly,
            offset(page, pageSize),
            limit(pageSize)
        );
        return PaginatedList.of(rows, NotificationsQueries.ListRow::totalCount, row -> {
            var n = row.playerNotifications();
            return new Notification(
                n.id(),
                n.key(),
                n.type(),
                n.createdAt(),
                n.expiresAt(),
                n.readAt(),
                Json.object(n.data())
            );
        });
    }

    @Override
    public boolean markRead(UUID id, boolean read) {
        return db.notifications.markRead(read, id) > 0;
    }

    @Override
    public boolean delete(UUID id) {
        return db.notifications.delete(id) > 0;
    }

    /// Go's `Manager.Create`: with `replace`, whatever the player already has under the same type
    /// and key goes first, so a repeated friend request is one notification, not a pile.
    public void create(
        ApiDatabase.Tx tx,
        UUID playerId,
        String type,
        String key,
        @Nullable JsonObject data,
        @Nullable Instant expiresAt,
        boolean replace
    ) {
        if (replace) tx.notifications.replace(type, key, playerId);
        tx.notifications.insert(
            new NotificationsQueries.InsertParams(
                UUID.randomUUID(),
                type,
                key,
                playerId,
                data == null ? null : data.toString(),
                expiresAt
            )
        );
        var update = new NotificationUpdate(NotificationUpdate.CREATE, playerId, type, key, data);
        tx.afterCommit(() -> nats.publish(update));
    }

    public void deleteByKey(ApiDatabase.Tx tx, UUID playerId, String type, String key) {
        if (tx.notifications.deleteByKey(type, key, playerId) == 0) return;
        var update = new NotificationUpdate(NotificationUpdate.DELETE, playerId, type, key, null);
        tx.afterCommit(() -> nats.publish(update));
    }

}
