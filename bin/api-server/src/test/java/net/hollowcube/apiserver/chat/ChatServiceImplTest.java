package net.hollowcube.apiserver.chat;

import com.sun.net.httpserver.HttpServer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.nats.client.Connection;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.chat.*;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The chat pipeline end to end: a real Postgres under the service, the generated server over it,
/// and the generated client talking to that over a real socket.
///
/// Going through the wire rather than calling the service directly is the point — the result
/// variants and the parts of a message are what every server reads, and this is the only way their
/// JSON is actually exercised.
class ChatServiceImplTest {

    @RegisterExtension
    // The schema lives with the queries in modules/api; this is the service on top of it.
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private static final String SENDER = "11111111-1111-1111-1111-111111111111";
    private static final String TARGET = "22222222-2222-2222-2222-222222222222";
    private static final String MAP = "33333333-3333-3333-3333-333333333333";

    /// Every publish the service made, in order, as (subject, body).
    private final List<Map.Entry<String, String>> sent = new ArrayList<>();

    private HttpServer server;
    private ChatClient chat;

    @BeforeEach
    void start() throws IOException {
        TEST_DB.seed("""
            insert into player_data (id, username, first_join, last_online, online) values
                ('%s', 'sender', now(), now(), true),
                ('%s', 'target', now(), now(), true);
            insert into player_sessions (player_id, proxy_id, skin_texture, skin_signature) values
                ('%s', 'proxy', '', ''),
                ('%s', 'proxy', '', '');
            insert into maps (id, owner, m_type, created_at, updated_at, file_id, opt_variant, opt_spawn_point, published_id)
                values ('%s', '%s', 'parkour', now(), now(), 'f', 'v', '{}', 7)"""
            .formatted(SENDER, TARGET, SENDER, TARGET, MAP, SENDER));

        var db = TEST_DB.database(ApiDatabase::new);
        var service = new ChatServiceImpl(db, recordingNats(sent));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(ChatServer.PATH, new ChatServer(service));
        server.start();

        chat = new ChatClient(HttpClient.newHttpClient(),
            "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    /// A publisher over a connection that records what it was handed instead of sending it, so the
    /// real subjects and the real bytes are what these assert on.
    private static NatsPublisher recordingNats(List<Map.Entry<String, String>> out) {
        var connection = (Connection) Proxy.newProxyInstance(
            ChatServiceImplTest.class.getClassLoader(), new Class<?>[]{Connection.class},
            (_, method, args) -> {
                if (method.getName().equals("publish") && args != null && args.length == 3) {
                    out.add(Map.entry((String) args[0], new String((byte[]) args[2], StandardCharsets.UTF_8)));
                    return null;
                }
                return method.getReturnType().isPrimitive() ? false : null;
            });
        return new NatsPublisher(connection, Wire.gson());
    }

    /// What went out on the subject servers read today.
    private List<ChatMessage> published() {
        return sent.stream()
            .filter(entry -> entry.getKey().equals(ChatMessage.SUBJECT))
            .map(entry -> Wire.gson().fromJson(entry.getValue(), ChatMessage.class))
            .toList();
    }

    /// What went out on the subject a server too old for the above reads.
    private List<JsonElement> publishedLegacy() {
        return sent.stream()
            .filter(entry -> entry.getKey().equals("chat.processed.global"))
            .map(entry -> JsonParser.parseString(entry.getValue()))
            .toList();
    }

    //region Global

    @Test
    void send_publishesAGlobalMessageAndLogsIt() throws SQLException {
        var result = chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hello world", null);

        assertEquals(new ChatResult.Sent(), result);
        // Everything but the seed, which the service mints so that one message renders the same
        // everywhere; what it is does not matter, only that it is on the message.
        var message = published().getFirst();
        assertEquals(new ChatMessage(SENDER, ChatChannel.GLOBAL, null, null,
            List.of(new MessagePart.Raw("hello world")), message.seed(), false), message);
        assertEquals(1, published().size());
        assertEquals(List.of("global|" + SENDER + "|hello world|null|null"), rows());
    }

    @Test
    void send_carriesWhetherTheSenderHasAHypercube() {
        TEST_DB.seed("update player_data set role = 'mod_1' where id = '" + SENDER + "'");

        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hi", null);

        assertTrue(published().getFirst().senderHasHypercube());
    }

    @Test
    void send_countsAnUnexpiredHypercubeAsOneToo() {
        TEST_DB.seed("update player_data set hypercube_start = now(), hypercube_end = now() + interval '1 day' where id = '" + SENDER + "'");

        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hi", null);

        assertTrue(published().getFirst().senderHasHypercube());
    }

    @Test
    void send_dropsAMessageWithNothingLeftInItAndLogsNothing() throws SQLException {
        var result = chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "🎉🎉", null);

        assertEquals(new ChatResult.Sent(), result);
        assertEquals(List.of(), published());
        assertEquals(List.of(), rows());
    }

    //endregion

    //region Censoring and mutes

    @Test
    void send_logsACensoredMessageWithWhatItMatchedAndPublishesNothing() throws SQLException {
        var result = chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "go fuck yourself", null);

        assertEquals(new ChatResult.Censored(), result);
        assertEquals(List.of(), published());
        // The message is logged as it was typed: what a moderator wants to read is what was said.
        assertEquals(List.of("global|" + SENDER + "|go fuck yourself|static-v2|fuck"), rows());
    }

    @Test
    void send_refusesAMutedPlayerBeforeAnythingElse() throws SQLException {
        mute("now() + interval '1 day'", null);

        var result = chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hello", null);

        // The expiry travels with the rejection so the sender can be told when they may talk again.
        var muted = assertInstanceOf(ChatResult.Muted.class, result);
        assertNotNull(muted.expiresAt());
        assertTrue(muted.expiresAt().isAfter(Instant.now()), "the mute should not have lifted yet");
        assertEquals(List.of(), published());
        assertEquals(List.of(), rows());
    }

    @Test
    void send_ignoresAnExpiredOrRevokedMute() {
        mute("now() - interval '1 day'", null);
        mute(null, "'" + TARGET + "'");

        assertEquals(new ChatResult.Sent(),
            chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hello", null));
    }

    @Test
    void send_treatsAMuteWithNoExpiryAsPermanent() {
        mute(null, null);

        // No expiry on the row, so none on the wire: this one never lifts.
        assertEquals(new ChatResult.Muted(null),
            chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hello", null));
    }

    @Test
    void send_appliesAMuteToSomeonePlayerDataHasNeverSeen() {
        // The mute is on punishments, which nothing makes conditional on having a player_data row.
        TEST_DB.seed("delete from player_data where id = '" + SENDER + "'");
        mute(null, null);

        assertEquals(new ChatResult.Muted(null),
            chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hello", null));
    }

    //endregion

    //region Direct messages

    @Test
    void send_deliversADirectMessageAndRemembersItBothWays() throws SQLException {
        var result = chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null);

        assertEquals(new ChatResult.Sent(), result);
        assertEquals(TARGET, published().getFirst().targetId());
        // Logged under Go's convention: the channel column holds the target for a direct message.
        assertEquals(List.of(TARGET + "|" + SENDER + "|psst|null|null"), rows());
        assertEquals(TARGET, replyTarget(SENDER));
        assertEquals(SENDER, replyTarget(TARGET));
    }

    @Test
    void send_resolvesAReplyToWhoeverWasLastSpokenTo() {
        chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null);
        sent.clear();

        var result = chat.send(TARGET, "server-1", ChatChannel.REPLY, null, "what", null);

        assertEquals(new ChatResult.Sent(), result);
        // A resolved reply is a direct message; nothing downstream has to know how it was typed.
        assertEquals(ChatChannel.DIRECT, published().getFirst().channel());
        assertEquals(SENDER, published().getFirst().targetId());
    }

    @Test
    void send_refusesAReplyWithNobodyToReplyTo() {
        var result = chat.send(SENDER, "server-1", ChatChannel.REPLY, null, "what", null);

        assertEquals(new ChatResult.TargetOffline(null), result);
        assertEquals(List.of(), published());
    }

    @Test
    void send_refusesADirectMessageToSomeoneWhoIsNotOnline() throws SQLException {
        TEST_DB.seed("delete from player_sessions where player_id = '" + TARGET + "'");

        var result = chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null);

        assertEquals(new ChatResult.TargetOffline(TARGET), result);
        assertEquals(List.of(), rows());
    }

    @Test
    void send_refusesAReplyToSomeoneWhoHasSinceGoneOffline() {
        chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null);
        TEST_DB.seed("delete from player_sessions where player_id = '" + SENDER + "'");

        assertEquals(new ChatResult.TargetOffline(SENDER),
            chat.send(TARGET, "server-1", ChatChannel.REPLY, null, "what", null));
    }

    @Test
    void send_refusesADirectMessageFromSomeoneWhoHasThemTurnedOff() {
        allowDms(SENDER, false);

        assertEquals(new ChatResult.DmDisabled(null),
            chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null));
    }

    @Test
    void send_refusesADirectMessageToSomeoneWhoHasThemTurnedOff() {
        allowDms(TARGET, false);

        // The target comes back so the sender can be told whose settings refused them, by name.
        assertEquals(new ChatResult.DmDisabled(TARGET),
            chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null));
    }

    @Test
    void send_treatsAnUnsetDirectMessageSettingAsOn() {
        assertEquals(new ChatResult.Sent(),
            chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null));
    }

    //endregion

    //region Local and [map]

    @Test
    void send_stampsALocalMessageWithTheSendersMap() {
        chat.send(SENDER, "server-1", ChatChannel.LOCAL, null, "hi", MAP);

        assertEquals(MAP, published().getFirst().mapId());
    }

    @Test
    void send_forwardsALocalMessageFromAServerThatSentNoMap() {
        // Servers on an old tag only send their map alongside `[map]`, and dropping their local chat
        // for the whole overlap would be worse than delivering it the old way.
        assertEquals(new ChatResult.Sent(),
            chat.send(SENDER, "server-1", ChatChannel.LOCAL, null, "hi", null));
        assertNull(published().getFirst().mapId());
    }

    @Test
    void send_doesNotStampAMapOnAnythingButLocal() {
        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hi", MAP);

        assertNull(published().getFirst().mapId());
    }

    @Test
    void send_resolvesTheMapTagToThePublishedMapTheSenderIsIn() {
        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "play [map]", MAP);

        assertEquals(List.of(new MessagePart.Raw("play "), new MessagePart.Map(MAP)),
            published().getFirst().parts());
    }

    @Test
    void send_refusesTheMapTagOutsideAPublishedMap() throws SQLException {
        TEST_DB.seed("update maps set published_id = null where id = '" + MAP + "'");

        assertEquals(new ChatResult.MapNotPublished(),
            chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "play [map]", MAP));
        assertEquals(List.of(), rows());
    }

    @Test
    void send_refusesTheMapTagWithNoMapAtAll() {
        assertEquals(new ChatResult.MapNotPublished(),
            chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "play [map]", null));
    }

    @Test
    void send_splitsOneMessageIntoEveryKindOfPart() {
        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "look :eyes: [map] at hollowcube.net", MAP);

        assertEquals(List.of(
            new MessagePart.Raw("look "), new MessagePart.Emoji("eyes"), new MessagePart.Raw(" "),
            new MessagePart.Map(MAP), new MessagePart.Raw(" at "), new MessagePart.Url("hollowcube.net")
        ), published().getFirst().parts());
    }

    //endregion

    @Test
    void send_refusesAChannelItDoesNotKnow() throws IOException, InterruptedException {
        // Not reachable through the client — UNKNOWN is what a channel this build does not know reads
        // as, and cannot be written back — so this is a caller from the future, posted by hand.
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + ChatServer.PATH + "/send"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("""
                {"senderId":"%s","serverId":"server-1","channel":"PARTY","message":"hi","seed":1}"""
                .formatted(SENDER)))
            .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertEquals(List.of(), published());
    }

    @Test
    void send_refusesASenderThatIsNotAUuid() {
        var failure = assertThrows(IpcException.class,
            () -> chat.send("nobody", "server-1", ChatChannel.GLOBAL, null, "hi", null));

        assertEquals(400, failure.status());
    }

    //endregion

    //region Legacy subject

    @Test
    void send_alsoWritesTheGoShapeOnTheSubjectOlderServersRead() {
        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "look :sus: [map] hollowcube.net", MAP);

        // Every number here is an ordinal a server on an older tag reads positionally, so none of
        // them may move: type 0 is an unsigned message, parts are raw/emoji/map/url in that order.
        var json = publishedLegacy().getFirst().getAsJsonObject();
        assertEquals(JsonParser.parseString("""
            [{"type":0,"text":"look "},{"type":1,"name":"sus"},{"type":0,"text":" "},
             {"type":2,"mapId":"%s"},{"type":0,"text":" "},{"type":3,"text":"hollowcube.net"}]"""
            .formatted(MAP)), json.get("parts"));
        assertEquals(0, json.get("type").getAsInt());
        assertEquals("global", json.get("channel").getAsString());
        assertEquals(SENDER, json.get("sender").getAsString());
    }

    @Test
    void send_putsADirectMessageTargetInTheLegacyChannel() {
        chat.send(SENDER, "server-1", ChatChannel.DIRECT, TARGET, "psst", null);

        assertEquals(TARGET, publishedLegacy().getFirst().getAsJsonObject().get("channel").getAsString());
    }

    @Test
    void send_leavesALocalMessageForTheOldServerToPlaceItself() {
        // Its map goes nowhere: a server that old works the audience out from the sender's session
        // presence, which is what it has always done.
        chat.send(SENDER, "server-1", ChatChannel.LOCAL, null, "hi", MAP);

        var json = publishedLegacy().getFirst().getAsJsonObject();
        assertEquals("local", json.get("channel").getAsString());
        assertEquals(7, json.size());
    }

    /// The copy is only there for a server that cannot hear [ChatMessage#SUBJECT]; one that can
    /// reads both subjects and would otherwise render the message twice.
    @Test
    void send_marksTheLegacyCopyAsOne() {
        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "hello", null);

        assertTrue(publishedLegacy().getFirst().getAsJsonObject().get("mirrored").getAsBoolean());
    }

    @Test
    void send_publishesNothingOnEitherSubjectWhenItRefuses() {
        chat.send(SENDER, "server-1", ChatChannel.GLOBAL, null, "go fuck yourself", null);

        assertEquals(List.of(), sent);
    }

    //endregion

    //region Command log

    private static CommandExecution execution(long at, String player, String command) {
        return new CommandExecution(Instant.ofEpochMilli(at), player, "server-1", "map-1", "world-1", command, false,
            CommandOutcome.SUCCESS, null, 3);
    }

    @Test
    void logCommand_storesEveryFieldOfARow() throws SQLException {
        chat.logCommand(execution(1_000L, SENDER, "spawn"));

        assertEquals(List.of("1970-01-01 00:00:01|" + SENDER + "|server-1|map-1|world-1|spawn|false|success|null|3"),
            commandRows());
    }

    @Test
    void logCommand_keepsEveryCommandOfARun() throws SQLException {
        chat.logCommand(execution(1_000L, SENDER, "one"));
        chat.logCommand(execution(2_000L, SENDER, "two"));
        chat.logCommand(execution(3_000L, SENDER, "three"));

        assertEquals(List.of("one", "two", "three"), commands());
    }

    @Test
    void logCommand_keepsTheOptionalColumnsEmptyForACommandRunOutsideAMap() throws SQLException {
        chat.logCommand(new CommandExecution(Instant.ofEpochMilli(1_000L), SENDER, "server-1", null, null, "spawn",
            false, CommandOutcome.SUCCESS, null, 3));

        assertEquals(List.of("1970-01-01 00:00:01|" + SENDER + "|server-1|null|null|spawn|false|success|null|3"),
            commandRows());
    }

    @Test
    void logCommand_keepsEveryOutcomeAndWhatWentWrongWithIt() throws SQLException {
        chat.logCommand(new CommandExecution(Instant.ofEpochMilli(1_000L), SENDER, "server-1", null, null, "tp", true,
            CommandOutcome.EXECUTION_ERROR, "java.lang.IllegalStateException: nope", 12));

        assertEquals(List.of("1970-01-01 00:00:01|" + SENDER + "|server-1|null|null|tp|true|execution_error"
            + "|java.lang.IllegalStateException: nope|12"), commandRows());
    }

    @Test
    void logCommand_keepsOnePlayersCommandsApartFromAnothers() throws SQLException {
        chat.logCommand(execution(1_000L, SENDER, "mine"));
        chat.logCommand(execution(2_000L, TARGET, "theirs"));

        assertEquals(List.of("mine"), commands("where player_id = '" + SENDER + "'"));
    }

    /// Every command row as `timestamp|playerId|serverId|mapId|instanceId|command|remote|outcome|error|durationMs`.
    private List<String> commandRows() throws SQLException {
        return commandQuery("""
            select to_char(timestamp at time zone 'utc', 'YYYY-MM-DD HH24:MI:SS') || '|' || player_id || '|' ||
                   server_id || '|' || coalesce(map_id, 'null') || '|' || coalesce(instance_id, 'null') || '|' ||
                   command || '|' || remote || '|' || outcome || '|' || coalesce(error, 'null') || '|' || duration_ms
            from command_log
            order by timestamp""");
    }

    private List<String> commands(String... where) throws SQLException {
        return commandQuery("select command from command_log " + String.join(" ", where) + " order by timestamp");
    }

    private List<String> commandQuery(String sql) throws SQLException {
        var out = new ArrayList<String>();
        try (var st = TEST_DB.conn().createStatement(); var rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    //endregion

    private void mute(String expiresAt, String revokedBy) {
        TEST_DB.seed("""
            insert into punishments (player_id, executor_id, type, created_at, comment, expires_at, revoked_by)
            values ('%s', '%s', 'mute', now(), 'because', %s, %s)"""
            .formatted(SENDER, TARGET, expiresAt == null ? "null" : expiresAt, revokedBy == null ? "null" : revokedBy));
    }

    private void allowDms(String player, boolean allowed) {
        TEST_DB.seed("update player_data set settings = '{\"allow_direct_messages\": %s}' where id = '%s'"
            .formatted(allowed, player));
    }

    /// Every logged message as `channel|sender|content|censoredBy|censoredDetail`.
    private List<String> rows() throws SQLException {
        var out = new ArrayList<String>();
        try (var st = TEST_DB.conn().createStatement();
             var rs = st.executeQuery("select channel, sender, content, censored_by, censored_detail "
                 + "from chat_messages order by timestamp")) {
            while (rs.next()) {
                out.add(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3)
                    + "|" + rs.getString(4) + "|" + rs.getString(5));
            }
        }
        return out;
    }

    private String replyTarget(String player) throws SQLException {
        try (var st = TEST_DB.conn().createStatement();
             var rs = st.executeQuery("select reply_target from player_sessions where player_id = '" + player + "'")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
