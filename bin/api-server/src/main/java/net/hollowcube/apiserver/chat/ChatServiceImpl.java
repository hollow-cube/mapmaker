package net.hollowcube.apiserver.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.ChatQueries;
import net.hollowcube.apiserver.db.CommandLogQueries;
import net.hollowcube.apiserver.db.PlayersQueries;
import net.hollowcube.apiserver.text.ChatText;
import net.hollowcube.apiserver.text.ProfanityFilter;
import net.hollowcube.ipc.chat.*;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static net.hollowcube.ipc.util.IpcArgs.uuid;

/// Everything that happens to a chat message, in one place.
///
/// It used to be spread across the sending server, a NATS work queue and the Go handler that drained
/// it, with every rejection travelling back to the player as a message published to their server. It
/// is one call now, and the rejections are its return value.
///
public final class ChatServiceImpl implements ChatService {

    /// What a message that mentions a map is written with.
    private static final String MAP_TAG = "[map]";
    /// Where the go handler published, read by every server too old to know [ChatMessage#SUBJECT].
    /// Delete this and everything named `legacy` below, plus the `CHAT_RAW`/`CHAT_PROCESSED`
    /// streams, once no tag that old is live.
    private static final String LEGACY_SUBJECT = "chat.processed.global";
    /// `model.ChatUnsigned`.
    private static final int TYPE_UNSIGNED = 0;
    /// `model.PartTypeRaw`, `PartTypeEmoji`, `PartTypeMap`, `PartTypeUrl`, in that order. Every enum
    /// on that path is an int because that is what go wrote and what those servers decode: their
    /// gson reads enums by ordinal.
    private static final int PART_RAW = 0;
    private static final int PART_EMOJI = 1;
    private static final int PART_MAP = 2;
    private static final int PART_URL = 3;

    /// Go's name for this filter, in `censored_by`. The Java one is a port of that engine, so a
    /// moderator reading old rows and new ones is reading the same thing.
    private static final String CENSOR_ENGINE = "static-v2";

    private final ApiDatabase db;
    private final NatsPublisher nats;

    public ChatServiceImpl(ApiDatabase db, NatsPublisher nats) {
        this.db = db;
        this.nats = nats;
    }

    @Override
    public ChatResult send(String senderId, String serverId, ChatChannel channel, @Nullable String targetId,
                           String message, @Nullable String currentMapId) {
        var sender = uuid(senderId, "senderId");
        var text = ChatText.strip(message);
        if (text.isEmpty()) return new ChatResult.Sent();

        // A reply resolves to a target here so that every direct message looks the same from here on,
        // and so that "who did I last talk to" is answered where the sessions are rather than in a
        // redis key nothing owned.
        var target = switch (channel) {
            case DIRECT -> uuid(targetId, "targetId");
            case REPLY -> db.chat.getReplyTarget(sender);
            case GLOBAL, LOCAL, STAFF -> null;
            case UNKNOWN -> throw new IpcException(400, "unknown chat channel");
        };
        // Nobody to reply to reads as an offline target with no name to give.
        if (channel == ChatChannel.REPLY && target == null) return new ChatResult.TargetOffline(null);

        // Everything the players themselves decide, in one read: the mute that stops the message,
        // the direct message settings, and whether the sender's emoji render for everyone — which
        // travels with the message rather than being asked per server.
        var people = db.players.getChatPlayers(target == null ? List.of(sender) : List.of(sender, target));
        var senderRow = find(people, sender);
        if (senderRow != null && senderRow.muted())
            return new ChatResult.Muted(senderRow.muteExpiresAt());

        if (target != null && !db.chat.playerOnline(target))
            return new ChatResult.TargetOffline(target.toString());
        if (target != null) {
            // Someone who has turned direct messages off does not get to send them either.
            if (!allowsDms(people, sender)) return new ChatResult.DmDisabled(null);
            if (!allowsDms(people, target))
                return new ChatResult.DmDisabled(target.toString());
        }

        // Only paid for by a message that mentions one. A server on an old tag sends its map only
        // when it has already checked this itself, so for those this is the same answer twice.
        var mentionsMap = text.contains(MAP_TAG);
        if (mentionsMap && (currentMapId == null || !db.maps.isMapPublished(uuid(currentMapId, "currentMapId"))))
            return new ChatResult.MapNotPublished();

        var censor = ProfanityFilter.test(text);
        db.chat.insertChatMessage(new ChatQueries.InsertChatMessageParams(
            serverId, channelColumn(channel, target), senderId, message,
            censor.matched() ? CENSOR_ENGINE : null,
            censor.matched() ? censor.matches().stream().map(ProfanityFilter.Match::term).collect(Collectors.joining(",")) : null));
        if (censor.matched()) return new ChatResult.Censored();

        publish(new ChatMessage(
            senderId,
            // A resolved reply is a direct message; nothing downstream should have to know it was
            // typed as `/r`.
            channel == ChatChannel.REPLY ? ChatChannel.DIRECT : channel,
            target == null ? null : target.toString(),
            channel == ChatChannel.LOCAL ? currentMapId : null,
            MessageTokenizer.tokenize(text, mentionsMap ? currentMapId : null),
            // The randomness in rendering is seeded once, here, so that every server draws the
            // same emoji for the same message.
            ThreadLocalRandom.current().nextLong(),
            hasHypercube(people, sender)));

        if (target != null) {
            // Both directions, so that answering someone makes you the person they reply to. Go only
            // did this for a message typed as a dm, which left a reply chain pointing at whoever
            // spoke first forever.
            db.chat.setReplyTargets(sender, target);
        }
        return new ChatResult.Sent();
    }

    @Override
    public void logCommand(CommandExecution execution) {
        db.commandLog.insertCommandLog(new CommandLogQueries.InsertCommandLogParams(
            execution.timestamp(), uuid(execution.playerId(), "playerId"), execution.serverId(),
            execution.mapId(), execution.instanceId(), execution.command(), execution.remote(),
            outcomeColumn(execution.outcome()), execution.error(), execution.durationMs()));
    }

    /// The check constraint's spelling of an outcome.
    private static String outcomeColumn(CommandOutcome outcome) {
        return switch (outcome.status()) {
            case SUCCESS -> "success";
            case DENIED -> "denied";
            case NOT_FOUND -> "not_found";
            case SYNTAX_ERROR -> "syntax_error";
            case EXECUTION_ERROR -> "execution_error";
            case UNKNOWN -> throw new IpcException(400, "unknown command outcome");
        };
    }

    /// Out to every server, on both subjects until the go path is gone.
    ///
    /// A game server ships on a release tag and outlives any number of api-server deploys, so for
    /// as long as one is running that only knows `chat.processed.global` it has to keep hearing
    /// what players on newer servers say. JetStream captures a core publish onto any stream whose
    /// subjects match, so the second one lands in `CHAT_PROCESSED` and reaches them exactly as
    /// before. Only servers that old read it: a server that knows the new subject reads that one
    /// alone, which is what keeps it from rendering the message twice.
    private void publish(ChatMessage message) {
        nats.publish(ChatMessage.SUBJECT, message);
        nats.publish(LEGACY_SUBJECT, encodeLegacyChat(message));
    }

    /// `message` as `model.ChatMessage` marshals it.
    private static JsonObject encodeLegacyChat(ChatMessage message) {
        var json = new JsonObject();
        json.addProperty("type", TYPE_UNSIGNED);
        json.addProperty("channel", legacyChannel(message));
        json.addProperty("sender", message.senderId());
        json.add("parts", legacyParts(message));
        json.addProperty("seed", message.seed());
        json.addProperty("senderHasHypercube", message.senderHasHypercube());
        return json;
    }

    /// Go's one channel string: a name, or the target's uuid for a direct message. An old server
    /// works out a local message's audience from the sender's session presence, as it always did.
    private static String legacyChannel(ChatMessage message) {
        return switch (message.channel()) {
            case GLOBAL -> "global";
            case LOCAL -> "local";
            case STAFF -> "staff";
            case DIRECT, REPLY -> Objects.requireNonNull(message.targetId(), "targetId");
            case UNKNOWN -> throw new IpcException(400, "unknown chat channel");
        };
    }

    private static JsonArray legacyParts(ChatMessage message) {
        var parts = new JsonArray();
        for (var part : message.parts()) {
            var json = new JsonObject();
            switch (part) {
                case MessagePart.Raw raw -> {
                    json.addProperty("type", PART_RAW);
                    json.addProperty("text", raw.text());
                }
                case MessagePart.Url url -> {
                    json.addProperty("type", PART_URL);
                    json.addProperty("text", url.text());
                }
                case MessagePart.Emoji emoji -> {
                    json.addProperty("type", PART_EMOJI);
                    json.addProperty("name", emoji.name());
                }
                case MessagePart.Map map -> {
                    json.addProperty("type", PART_MAP);
                    json.addProperty("mapId", map.mapId());
                }
                // Nothing here builds one: the parts were made by the service that is publishing them.
                case MessagePart.Unknown _ -> {
                    continue;
                }
            }
            parts.add(json);
        }
        return parts;
    }

    /// The `channel` column, in the shape Go writes it: a name, or the target's uuid for a direct
    /// message, so that old rows and new ones read alike.
    private static String channelColumn(ChatChannel channel, @Nullable UUID target) {
        return switch (channel) {
            case GLOBAL -> "global";
            case LOCAL -> "local";
            case STAFF -> "staff";
            case DIRECT, REPLY -> Objects.requireNonNull(target, "targetId").toString();
            case UNKNOWN -> throw new IpcException(400, "unknown chat channel");
        };
    }

    /// Defaults to on, for the setting and for a player with no row at all.
    private static boolean allowsDms(List<PlayersQueries.GetChatPlayersRow> people, UUID id) {
        var row = find(people, id);
        return row == null || row.allowDms();
    }

    private static boolean hasHypercube(List<PlayersQueries.GetChatPlayersRow> people, UUID id) {
        var row = find(people, id);
        return row != null && row.hypercube();
    }

    private static PlayersQueries.@Nullable GetChatPlayersRow find(List<PlayersQueries.GetChatPlayersRow> people, UUID id) {
        for (var row : people) {
            if (row.id().equals(id)) return row;
        }
        return null;
    }
}
