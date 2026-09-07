package net.hollowcube.apiserver.player;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.apiserver.common.RecordingNats;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.notification.NotificationServiceImpl;
import net.hollowcube.ipc.player.BlockResult;
import net.hollowcube.ipc.player.FriendRequestResult;
import net.hollowcube.ipc.player.SocialClient;
import net.hollowcube.ipc.player.SocialServer;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialServiceImplTest {

    @RegisterExtension
    // TRUNCATE: every write goes through `db.txResult`, which cannot commit inside a transaction
    // the harness is holding open.
    static final TestDb TEST_DB = TestDb.of(
        "../../modules/api/src/main/sql/migrations",
        TestDb.Mode.TRUNCATE
    );

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CAROL = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DAVE = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MOD = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID NOBODY = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private HttpServer server;
    private RecordingNats nats;
    private SocialClient social;

    @BeforeEach
    void start() throws IOException {
        // Alice and Bob are friends; Carol is online and visible; Dave is online but vanished;
        // Bob has a session too. Mod is staff.
        TEST_DB.seed(
            """
            insert into player_data (id, username, first_join, last_online, online, settings, role) values
                ('%1$s', 'Alice', now(), now() - interval '5 day', false, '{}', 'default'),
                ('%2$s', 'Bob', now(), now() - interval '4 day', false, '{}', 'default'),
                ('%3$s', 'Carol', now(), now() - interval '3 day', false, '{}', 'default'),
                ('%4$s', 'Dave', now(), now() - interval '2 day', false, '{"auto_reject_friend_requests": true}', 'default'),
                ('%5$s', 'Moddy', now(), now() - interval '1 day', false, '{}', 'mod_1');
            insert into player_sessions (player_id, proxy_id, skin_texture, skin_signature, hidden) values
                ('%2$s', 'proxy', '', '', false),
                ('%3$s', 'proxy', '', '', false),
                ('%4$s', 'proxy', '', '', true);
            insert into player_friends (player_id, target_id, created_at) values
                ('%1$s', '%2$s', now() - interval '1 hour'),
                ('%2$s', '%1$s', now() - interval '1 hour'),
                ('%1$s', '%4$s', now() - interval '2 hour'),
                ('%4$s', '%1$s', now() - interval '2 hour'),
                ('%1$s', '%5$s', now() - interval '3 hour'),
                ('%5$s', '%1$s', now() - interval '3 hour')"""
                .formatted(ALICE, BOB, CAROL, DAVE, MOD)
        );

        var db = TEST_DB.database(ApiDatabase::new);
        nats = new RecordingNats();
        var service = new SocialServiceImpl(db, new NotificationServiceImpl(db, nats.publisher));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(SocialServer.PATH, new SocialServer(service));
        server.start();
        social = new SocialClient(
            HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private long count(String sql) throws SQLException {
        try (var st = TEST_DB.conn().createStatement(); var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    //region Friends

    @Test
    void friends_onlineFirstFromSessionsNotTheColumn() {
        var page = social.friends(ALICE, false, 0, 10);
        assertEquals(3, page.count());
        // Bob is the one visible session among Alice's friends; Dave is vanished, Mod is offline.
        assertEquals(
            List.of(BOB, MOD, DAVE),
            page.results().stream().map(f -> f.player().id()).toList()
        );
        assertTrue(page.results().getFirst().online());
        assertFalse(page.results().get(1).online());
        assertEquals("mod_1", page.results().get(1).player().displayName().badge());

        var online = social.friends(ALICE, true, 0, 10);
        assertEquals(1, online.count());
        assertEquals(BOB, online.first().player().id());

        assertEquals(List.of(BOB), social.onlineFriendIds(ALICE));
        assertTrue(social.onlineFriendIds(DAVE).isEmpty());
        assertTrue(social.onlineFriendIds(CAROL).isEmpty());
    }

    @Test
    void removeFriend_bothRowsOnce() throws SQLException {
        assertTrue(social.removeFriend(BOB, ALICE));
        assertEquals(
            0,
            count(
                "select count(*) from player_friends where player_id in ('%s','%s') and target_id in ('%s','%s')".formatted(
                    ALICE,
                    BOB,
                    ALICE,
                    BOB
                )
            )
        );
        assertFalse(social.removeFriend(ALICE, BOB));
    }

    //endregion

    //region Requests

    @Test
    void sendFriendRequest_sendsThenTheOtherSideAccepts() throws SQLException {
        assertInstanceOf(FriendRequestResult.Sent.class, social.sendFriendRequest(ALICE, CAROL));
        assertInstanceOf(
            FriendRequestResult.AlreadyRequested.class,
            social.sendFriendRequest(ALICE, CAROL)
        );
        assertEquals(
            1,
            count(
                "select count(*) from player_notifications where player_id = '%s' and type = 'friend_request' and key = '%s' and deleted_at is null".formatted(
                    CAROL,
                    ALICE
                )
            )
        );
        assertEquals(1, nats.on("notification.created").size());

        var outgoing = social.outgoingFriendRequests(ALICE, 0, 10);
        assertEquals(1, outgoing.count());
        assertEquals(CAROL, outgoing.first().player().id());
        var incoming = social.incomingFriendRequests(CAROL, 0, 10);
        assertEquals(ALICE, incoming.first().player().id());
        assertTrue(social.outgoingFriendRequests(CAROL, 0, 10).isEmpty());

        assertInstanceOf(
            FriendRequestResult.Accepted.class,
            social.sendFriendRequest(CAROL, ALICE)
        );
        assertEquals(
            2,
            count(
                "select count(*) from player_friends where (player_id = '%s' and target_id = '%s') or (player_id = '%s' and target_id = '%s')".formatted(
                    ALICE,
                    CAROL,
                    CAROL,
                    ALICE
                )
            )
        );
        assertEquals(0, count("select count(*) from player_friend_requests"));
        assertEquals(
            0,
            count(
                "select count(*) from player_notifications where type = 'friend_request' and deleted_at is null"
            )
        );
        assertEquals(
            2,
            count("select count(*) from player_notifications where type = 'friend_added'")
        );
        assertEquals(1, nats.on("notification.deleted").size());
        assertEquals(3, nats.on("notification.created").size());

        assertInstanceOf(
            FriendRequestResult.AlreadyFriends.class,
            social.sendFriendRequest(ALICE, CAROL)
        );
    }

    @Test
    void sendFriendRequest_refusals() {
        assertInstanceOf(
            FriendRequestResult.TargetAutoRejects.class,
            social.sendFriendRequest(CAROL, DAVE)
        );

        assertInstanceOf(BlockResult.Blocked.class, social.block(CAROL, BOB));
        assertInstanceOf(
            FriendRequestResult.BlockedTarget.class,
            social.sendFriendRequest(CAROL, BOB)
        );
        assertInstanceOf(
            FriendRequestResult.BlockedByTarget.class,
            social.sendFriendRequest(BOB, CAROL)
        );

        assertEquals(
            404,
            assertThrows(IpcException.class, () -> social.sendFriendRequest(ALICE, NOBODY)).status()
        );
        assertEquals(
            400,
            assertThrows(IpcException.class, () -> social.sendFriendRequest(ALICE, ALICE)).status()
        );
    }

    @Test
    void sendFriendRequest_limitCountsFriendsAndOutgoing() throws SQLException {
        var seed = new StringBuilder(
            "insert into player_data (id, username, first_join, last_online, online) values "
        );
        var requests = new StringBuilder(
            "insert into player_friend_requests (player_id, target_id) values "
        );
        for (int i = 0; i < 27; i++) {
            var id = "66666666-6666-6666-6666-%012d".formatted(i);
            seed.append(i == 0 ? "" : ",")
                .append("('%s', 'x%d', now(), now(), false)".formatted(id, i));
            requests.append(i == 0 ? "" : ",").append("('%s', '%s')".formatted(ALICE, id));
        }
        TEST_DB.seed(seed.toString());
        TEST_DB.seed(requests.toString());

        // 3 friends + 27 outgoing = 30, the free limit.
        var limited = assertInstanceOf(
            FriendRequestResult.LimitReached.class,
            social.sendFriendRequest(ALICE, CAROL)
        );
        assertEquals(30, limited.limit());
        assertEquals(3, limited.friendCount());
        assertEquals(27, limited.outgoingRequestCount());

        TEST_DB.seed(
            "update player_data set hypercube_end = now() + interval '1 day' where id = '%s'".formatted(
                ALICE
            )
        );
        assertInstanceOf(FriendRequestResult.Sent.class, social.sendFriendRequest(ALICE, CAROL));
    }

    @Test
    void deleteFriendRequest_eitherWayClearsTheNotification() throws SQLException {
        social.sendFriendRequest(ALICE, CAROL);
        nats.sent.clear();

        // Carol declines: the request was incoming for her, and her notification goes.
        var declined = social.deleteFriendRequest(CAROL, ALICE, true);
        assertNotNull(declined);
        assertEquals(ALICE, declined.player().id());
        assertEquals(
            0,
            count(
                "select count(*) from player_notifications where type = 'friend_request' and deleted_at is null"
            )
        );
        assertEquals(1, nats.on("notification.deleted").size());
        assertNull(social.deleteFriendRequest(CAROL, ALICE, true));

        // Alice withdraws: outgoing for her, Carol's notification goes.
        social.sendFriendRequest(ALICE, CAROL);
        var withdrawn = social.deleteFriendRequest(ALICE, CAROL, false);
        assertNotNull(withdrawn);
        assertEquals(CAROL, withdrawn.player().id());
        assertEquals(
            0,
            count(
                "select count(*) from player_notifications where type = 'friend_request' and deleted_at is null"
            )
        );

        // Not bidirectional: Carol cannot delete Alice's request as if it were her own.
        social.sendFriendRequest(ALICE, CAROL);
        assertNull(social.deleteFriendRequest(CAROL, ALICE, false));
        assertEquals(1, count("select count(*) from player_friend_requests"));
    }

    //endregion

    //region Blocks

    @Test
    void block_endsTheFriendshipAndPendingRequests() throws SQLException {
        social.sendFriendRequest(CAROL, ALICE);
        nats.sent.clear();

        assertInstanceOf(BlockResult.Blocked.class, social.block(ALICE, BOB));
        assertInstanceOf(BlockResult.AlreadyBlocked.class, social.block(ALICE, BOB));
        assertEquals(
            0,
            count(
                "select count(*) from player_friends where player_id in ('%s','%s') and target_id in ('%s','%s')".formatted(
                    ALICE,
                    BOB,
                    ALICE,
                    BOB
                )
            )
        );

        assertInstanceOf(BlockResult.Blocked.class, social.block(ALICE, CAROL));
        assertEquals(0, count("select count(*) from player_friend_requests"));
        assertEquals(
            0,
            count(
                "select count(*) from player_notifications where type = 'friend_request' and deleted_at is null"
            )
        );
        assertEquals(1, nats.on("notification.deleted").size());

        assertInstanceOf(BlockResult.TargetIsStaff.class, social.block(ALICE, MOD));
        assertEquals(
            404,
            assertThrows(IpcException.class, () -> social.block(ALICE, NOBODY)).status()
        );

        var blocks = social.blocks(ALICE, 0, 10);
        assertEquals(2, blocks.count());
        assertEquals(
            List.of(CAROL, BOB),
            blocks.results().stream().map(b -> b.target().id()).toList()
        );
        assertEquals(ALICE, blocks.first().blockerId());

        assertTrue(social.unblock(ALICE, BOB));
        assertFalse(social.unblock(ALICE, BOB));
        assertEquals(1, social.blocks(ALICE, 0, 10).count());
    }

    @Test
    void blocks_hideStaffInThePageAndTheTotal() {
        TEST_DB.seed(
            "insert into player_blocks (player_id, target_id) values ('%s', '%s'), ('%s', '%s')".formatted(
                ALICE,
                MOD,
                ALICE,
                CAROL
            )
        );
        var blocks = social.blocks(ALICE, 0, 10);
        assertEquals(1, blocks.count());
        assertEquals(CAROL, blocks.first().target().id());
    }

    @Test
    void blocksBetween_directionAndBidirectional() {
        social.block(ALICE, CAROL);
        assertEquals(1, social.blocksBetween(ALICE, CAROL, false).size());
        assertTrue(social.blocksBetween(CAROL, ALICE, false).isEmpty());

        var between = social.blocksBetween(CAROL, ALICE, true);
        assertEquals(1, between.size());
        assertEquals(ALICE, between.getFirst().blockerId());
        assertEquals(CAROL, between.getFirst().target().id());
    }

    //endregion
}
