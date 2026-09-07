package net.hollowcube.ipc.notification;

import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.util.Ipc;

import java.util.UUID;

@Ipc
public interface NotificationService {

    PaginatedList<Notification> list(UUID playerId, boolean unreadOnly, int page, int pageSize);

    boolean markRead(UUID id, boolean read);

    boolean delete(UUID id);
}
