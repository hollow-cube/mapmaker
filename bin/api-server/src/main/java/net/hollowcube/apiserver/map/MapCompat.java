package net.hollowcube.apiserver.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.hollowcube.apiserver.common.Json;
import net.hollowcube.apiserver.db.MapSlots;
import net.hollowcube.apiserver.db.MapStats;
import net.hollowcube.apiserver.db.Maps;
import net.hollowcube.apiserver.db.MapsQueries;
import net.hollowcube.apiserver.db.SaveStates;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.map.MapBuilder;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.ipc.map.MapDifficulty;
import net.hollowcube.ipc.map.MapLeaderboard;
import net.hollowcube.ipc.map.MapQuality;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.map.MapVariant;
import net.hollowcube.ipc.map.MapVerification;
import net.hollowcube.ipc.map.PlayerMapProgress;
import net.hollowcube.ipc.map.SaveStateData;
import net.hollowcube.ipc.map.SaveStateType;
import net.hollowcube.ipc.util.Position;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;
import static net.hollowcube.ipc.util.IpcException.badRequest;

/// What the Go api-server and this one have to spell identically: the column values of `maps`, the
/// events on `map.*` and `notification.*`, and how a row becomes the [MapData] every server reads.
/// Transcribed from `api/v4Internal/server_maps.go` and `internal/mapdb`; both servers write these
/// tables for as long as both are deployed.
final class MapCompat {

    private static final Logger logger = LoggerFactory.getLogger(MapCompat.class);

    /// `maps.verification`.
    static final long VERIFICATION_NONE = 0;
    static final long VERIFICATION_PENDING = 1;
    static final long VERIFICATION_VERIFIED = 2;

    /// The column default, which is what a row Go wrote before the column existed reads as.
    static final int DEFAULT_PROTOCOL_VERSION = 769;

    /// Go's `MapManagementAction`, and the subjects it goes out on.
    static final int MAP_ACTION_DELETE = 1;
    static final int MAP_ACTION_DRAIN = 2;
    static final String MAP_DELETE_SUBJECT = "map.delete";
    static final String MAP_DRAIN_SUBJECT = "map.drain";

    /// Where a map server is told to move a player it may no longer keep.
    static final String PLAYER_TRANSFER_SUBJECT = "player.transfer";

    /// Go's notification events and the two notification types the map service writes.
    static final String BUILDER_INVITE_NOTIFICATION = "map_builder_invite";
    static final String BUILDER_REJECTED_NOTIFICATION = "map_builder_rejected";

    /// `deleted_reason` for an owner deleting their own unpublished map, the one case that needs
    /// no reason given.
    static final String USER_DELETION = "user_deletion";

    /// The notification a player gets when staff strike their time on a map; its key is the map id.
    static final String MAP_TIME_DELETED_NOTIFICATION = "map_time_deleted";

    /// A game tick, which is what a time board resolves to.
    static final long TICK_MILLIS = 50;

    private MapCompat() {}

    /// `stats` is null for an unpublished map, which has none.
    static MapData mapData(Maps map, List<String> tags, @Nullable MapStats stats) {
        return mapData(
            map,
            tags,
            stats == null ? 0 : stats.playCount(),
            stats == null ? 0 : stats.winCount()
        );
    }

    static MapData mapData(Maps map, List<String> tags, long plays, long wins) {
        var winRate = plays == 0 ? 0.0 : (double) wins / plays;
        return new MapData(
            map.id(),
            map.owner(),
            requireNonNullElse(map.protocolVersion(), DEFAULT_PROTOCOL_VERSION),
            new MapData.Settings(
                requireNonNullElse(map.optName(), ""),
                requireNonNullElse(map.optIcon(), ""),
                MapSize.fromId(map.size()),
                variant(map),
                map.optSubvariant(),
                Wire.gson().fromJson(map.optSpawnPoint(), Position.class),
                tags,
                leaderboard(map.leaderboard()),
                extra(map)
            ),
            verification(map),
            map.publishedId() == null ? null : MapData.formatPublishedId(map.publishedId()),
            map.publishedAt(),
            map.listed(),
            MapQuality.fromId(requireNonNullElse(map.qualityOverride(), 0L).intValue()),
            difficulty(variant(map), plays, winRate),
            plays,
            winRate,
            map.totalLikes(),
            map.contest(),
            map.createdAt(),
            map.updatedAt()
        );
    }

    /// Anything that is not a building map is a parkour one; adventure maps were never written.
    static MapVariant variant(Maps map) {
        return "building".equals(map.optVariant()) ? MapVariant.BUILDING : MapVariant.PARKOUR;
    }

    static MapVerification verification(Maps map) {
        var value = requireNonNullElse(map.verification(), VERIFICATION_NONE);
        if (value == VERIFICATION_PENDING) return MapVerification.PENDING;
        if (value == VERIFICATION_VERIFIED) return MapVerification.VERIFIED;
        return MapVerification.UNVERIFIED;
    }

    /// The number the search filter compares against: Go's `mapDifficultyIndex`, where unrated
    /// is -1 and the rest count up from easy.
    static long difficultyId(MapDifficulty difficulty) {
        return switch (difficulty) {
            case UNRATED, UNKNOWN -> -1;
            case EASY -> 0;
            case MEDIUM -> 1;
            case HARD -> 2;
            case EXPERT -> 3;
            case NIGHTMARE -> 4;
        };
    }

    static MapDifficulty difficulty(MapVariant variant, long plays, double winRate) {
        if (plays < MapData.MIN_PLAYS_FOR_DIFFICULTY || variant != MapVariant.PARKOUR)
            return MapDifficulty.UNRATED;
        if (winRate < .05) return MapDifficulty.NIGHTMARE;
        if (winRate < .25) return MapDifficulty.EXPERT;
        if (winRate < .5) return MapDifficulty.HARD;
        if (winRate < .75) return MapDifficulty.MEDIUM;
        return MapDifficulty.EASY;
    }

    /// `opt_extra` with the dedicated boolean columns folded in, which is how every server reads
    /// them.
    static JsonObject extra(Maps map) {
        var extra = map.optExtra() == null
            ? new JsonObject()
            : JsonParser.parseString(new String(map.optExtra(), StandardCharsets.UTF_8))
                .getAsJsonObject();
        if (Boolean.TRUE.equals(map.optOnlySprint())) extra.addProperty("only_sprint", true);
        if (Boolean.TRUE.equals(map.optNoSprint())) extra.addProperty("no_sprint", true);
        if (Boolean.TRUE.equals(map.optNoJump())) extra.addProperty("no_jump", true);
        if (Boolean.TRUE.equals(map.optNoSneak())) extra.addProperty("no_sneak", true);
        if (Boolean.TRUE.equals(map.optBoat())) extra.addProperty("boat", true);
        return extra;
    }

    /// The `leaderboard` column, which is not a wire value even though it holds a wire record:
    /// Go spells `format` lower case and [Wire#gson()] spells an enum with the constant name, so
    /// reading it with that gson gives `UNKNOWN` for every map that has one.
    ///
    /// Null is the default leaderboard, which is what every map made before they were configurable
    /// has.
    static MapLeaderboard leaderboard(@Nullable String column) {
        if (column == null) return MapLeaderboard.DEFAULT;
        var json = Json.object(column);
        return new MapLeaderboard(
            json.get("asc").getAsBoolean(),
            // A format Go grows that this build does not have still has to read, or the map it is
            // on cannot be fetched at all.
            switch (json.get("format").getAsString()) {
                case "time" -> MapLeaderboard.Format.TIME;
                case "percent" -> MapLeaderboard.Format.PERCENT;
                case "number" -> MapLeaderboard.Format.NUMBER;
                default -> MapLeaderboard.Format.UNKNOWN;
            },
            json.get("score").getAsString()
        );
    }

    static @Nullable String leaderboardColumn(MapLeaderboard board) {
        var isDefault = board.asc()
            && board.format() == MapLeaderboard.Format.TIME
            && "q.playtime".equalsIgnoreCase(board.score().strip());
        if (isDefault) return null;
        var format = switch (board.format()) {
            case TIME -> "time";
            case PERCENT -> "percent";
            case NUMBER -> "number";
            // `MapPatch` rejects it, so this is only reachable from a caller that skipped it.
            case UNKNOWN -> throw new IllegalArgumentException("cannot write an unknown format");
        };
        var json = new JsonObject();
        json.addProperty("asc", board.asc());
        json.addProperty("format", format);
        json.addProperty("score", board.score());
        return json.toString();
    }

    static PlayerMapProgress progress(MapsQueries.GetMultiMapProgressRow row) {
        return new PlayerMapProgress(
            row.mapId(),
            row.completed()
                ? PlayerMapProgress.Progress.COMPLETE
                : PlayerMapProgress.Progress.STARTED,
            row.playtime()
        );
    }

    /// A map's board: a sorted set of every player's best score. The key is what Go's
    /// `mapLeaderboardKey(mapId, "playtime")` spells, and the member is what Go writes through
    /// `common.UUIDToBin`, the player's uuid as sixteen big-endian bytes rather than its text.
    static byte[] leaderboardKey(UUID mapId) {
        return ("map:" + mapId + ":lb_playtime").getBytes(StandardCharsets.UTF_8);
    }

    static byte[] leaderboardMember(UUID playerId) {
        return ByteBuffer.allocate(16)
            .putLong(playerId.getMostSignificantBits())
            .putLong(playerId.getLeastSignificantBits())
            .array();
    }

    static UUID leaderboardMember(byte[] member) {
        var buffer = ByteBuffer.wrap(member);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    /// A completed run's score: what it was scored, or its playtime for runs from before boards
    /// were configurable, which is also how the queries order them.
    static double score(SaveStates state) {
        return state.score() != null
            ? state.score()
            : Math.max(state.playtime(), state.ticks() * TICK_MILLIS);
    }

    static SaveStateType saveStateType(net.hollowcube.apiserver.db.SaveStateType type) {
        return switch (type) {
            case EDITING -> SaveStateType.EDITING;
            case PLAYING -> SaveStateType.PLAYING;
            case VERIFYING -> SaveStateType.VERIFYING;
        };
    }

    static net.hollowcube.apiserver.db.SaveStateType saveStateColumn(SaveStateType type) {
        return switch (type) {
            case EDITING -> net.hollowcube.apiserver.db.SaveStateType.EDITING;
            case PLAYING -> net.hollowcube.apiserver.db.SaveStateType.PLAYING;
            case VERIFYING -> net.hollowcube.apiserver.db.SaveStateType.VERIFYING;
            case UNKNOWN -> throw badRequest("unknown save state type");
        };
    }

    /// `state_v2` holds json text; a row Go could not read it answered with an empty state rather
    /// than refusing the player their map, and so does this.
    static SaveStateData saveStateData(SaveStates state) {
        JsonObject data = null;
        if (!state.completed()) {
            try {
                var json = JsonParser.parseString(
                    new String(state.stateV2(), StandardCharsets.UTF_8)
                );
                data = json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
            } catch (JsonParseException e) {
                logger.error("save state {} holds unreadable state", state.id(), e);
                data = new JsonObject();
            }
        }
        return new SaveStateData(
            state.id(),
            state.mapId(),
            state.playerId(),
            saveStateType(state.type()),
            state.created(),
            state.updated(),
            state.dataVersion(),
            requireNonNullElse(state.protocolVersion(), DEFAULT_PROTOCOL_VERSION),
            state.playtime(),
            state.ticks(),
            state.resets(),
            // Rows from before the column existed have it at zero; playtime is the floor.
            Math.max(state.totalPlaytime(), state.playtime()),
            state.completed(),
            state.completed() ? Double.valueOf(score(state)) : state.score(),
            data
        );
    }

    static MapBuilder builder(MapSlots slot) {
        return new MapBuilder(slot.playerId(), slot.createdAt(), slot.isPending());
    }

    static String inviteKey(UUID mapId) {
        return "map_builder_invites_" + mapId;
    }

    static JsonObject mapEvent(int action, UUID id) {
        var event = new JsonObject();
        event.addProperty("action", action);
        event.addProperty("id", id.toString());
        return event;
    }

}
