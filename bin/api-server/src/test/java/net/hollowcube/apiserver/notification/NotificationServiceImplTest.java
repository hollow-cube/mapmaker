package net.hollowcube.apiserver.notification;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.common.RecordingNats;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.notification.Notification;
import net.hollowcube.ipc.notification.NotificationClient;
import net.hollowcube.ipc.notification.NotificationServer;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceImplTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of(
        "../../modules/api/src/main/sql/migrations",
        TestDb.Mode.TRUNCATE
    );

    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private HttpServer server;
    private ApiDatabase db;
    private RecordingNats nats;
    private NotificationServiceImpl service;
    private NotificationClient notifications;

    @BeforeEach
    void start() throws IOException {
        TEST_DB.seed(
            """
            insert into player_data (id, username, first_join, last_online, online) values ('%s', 'p', now(), now(), true);
            insert into player_notifications (id, player_id, type, key, data, created_at, read_at, expires_at, deleted_at) values
                ('aaaaaaaa-0000-0000-0000-000000000001', '%s', 'update', 'v1', '{"link": "x"}', now() - interval '3 hour', null, null, null),
                ('aaaaaaaa-0000-0000-0000-000000000002', '%s', 'update', 'v2', 'null', now() - interval '2 hour', now(), null, null),
                ('aaaaaaaa-0000-0000-0000-000000000003', '%s', 'update', 'v3', null, now() - interval '1 hour', null, now() - interval '1 minute', null),
                ('aaaaaaaa-0000-0000-0000-000000000004', '%s', 'update', 'v4', null, now(), null, null, now())"""
                .formatted(PLAYER, PLAYER, PLAYER, PLAYER, PLAYER)
        );

        db = TEST_DB.database(ApiDatabase::new);
        nats = new RecordingNats();
        service = new NotificationServiceImpl(db, nats.publisher);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(NotificationServer.PATH, new NotificationServer(service));
        server.start();
        notifications = new NotificationClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void list_newestFirstWithoutExpiredOrDeleted() {
        var all = notifications.list(PLAYER, false, 0, 10);
        assertEquals(2, all.count());
        assertEquals(List.of("v2", "v1"), all.results().stream().map(Notification::key).toList());
        assertEquals("x", all.results().getLast().data().get("link").getAsString());
        assertTrue(all.results().getFirst().data().isEmpty());
        assertNotNull(all.results().getFirst().readAt());

        var unread = notifications.list(PLAYER, true, 0, 10);
        assertEquals(1, unread.count());
        assertEquals("v1", unread.first().key());
    }

    @Test
    void markRead_bothWays() {
        assertTrue(
            notifications.markRead(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"), true)
        );
        assertEquals(0, notifications.list(PLAYER, true, 0, 10).count());
        assertTrue(
            notifications.markRead(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002"), false)
        );
        assertEquals(1, notifications.list(PLAYER, true, 0, 10).count());
        assertFalse(
            notifications.markRead(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004"), true)
        );
        assertFalse(
            notifications.markRead(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000099"), true)
        );
    }

    @Test
    void delete_isSoftAndOnce() {
        assertTrue(notifications.delete(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")));
        assertFalse(notifications.delete(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")));
        assertEquals(1, notifications.list(PLAYER, false, 0, 10).count());
    }

    @Test
    void create_replacesAndPublishesGoShape() {
        db.tx(tx -> {
            service.create(tx, PLAYER, "friend_request", "k", null, null, true);
            assertTrue(nats.sent.isEmpty());
        });
        db.tx(tx -> service.create(tx, PLAYER, "friend_request", "k", null, null, true));

        var rows = notifications.list(PLAYER, false, 0, 10)
            .results()
            .stream()
            .filter(n -> n.type().equals("friend_request"))
            .toList();
        assertEquals(1, rows.size());

        var published = nats.on("notification.created");
        assertEquals(2, published.size());
        var message = published.getFirst().getAsJsonObject();
        assertEquals("create", message.get("action").getAsString());
        assertEquals(PLAYER.toString(), message.get("playerId").getAsString());
        assertEquals("friend_request", message.get("type").getAsString());
        assertEquals("k", message.get("key").getAsString());

        db.tx(tx -> {
            service.deleteByKey(tx, PLAYER, "friend_request", "k");
            assertTrue(nats.on("notification.deleted").isEmpty());
        });
        db.tx(tx -> service.deleteByKey(tx, PLAYER, "friend_request", "k"));
        assertEquals(1, nats.on("notification.deleted").size());
        assertEquals(
            0,
            notifications.list(PLAYER, false, 0, 10)
                .results()
                .stream()
                .filter(n -> n.type().equals("friend_request"))
                .count()
        );
    }

    @Test
    void create_rollbackDiscardsWritesAndPublications() {
        assertThrows(
            IllegalStateException.class,
            () -> db.tx(tx -> {
                service.create(tx, PLAYER, "friend_request", "k", null, null, true);
                service.deleteByKey(tx, PLAYER, "update", "v1");
                throw new IllegalStateException("abort");
            })
        );

        assertTrue(nats.sent.isEmpty());
        assertEquals(
            List.of("v2", "v1"),
            notifications.list(PLAYER, false, 0, 10)
                .results()
                .stream()
                .map(Notification::key)
                .toList()
        );

        db.tx(tx -> service.create(tx, PLAYER, "friend_request", "next", null, null, true));
        assertEquals(1, nats.sent.size());
        assertEquals(
            "next",
            nats.on("notification.created").getFirst().getAsJsonObject().get("key").getAsString()
        );
    }

}
