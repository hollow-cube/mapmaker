package net.hollowcube.apiserver.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hollowcube.apiserver.db.MapSlots;
import net.hollowcube.apiserver.db.MapStats;
import net.hollowcube.apiserver.db.Maps;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.map.MapBuilder;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.ipc.map.MapDifficulty;
import net.hollowcube.ipc.map.MapLeaderboard;
import net.hollowcube.ipc.map.MapQuality;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.map.MapVariant;
import net.hollowcube.ipc.map.MapVerification;
import net.hollowcube.ipc.util.Position;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/// What the Go api-server and this one have to spell identically: the column values of `maps`, the
/// events on `map.*` and `notification.*`, and how a row becomes the [MapData] every server reads.
/// Transcribed from `api/v4Internal/server_maps.go` and `internal/mapdb`; both servers write these
/// tables for as long as both are deployed.
final class MapCompat {

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
    static final String NOTIFICATION_CREATED_SUBJECT = "notification.created";
    static final String NOTIFICATION_DELETED_SUBJECT = "notification.deleted";
    static final String BUILDER_INVITE_NOTIFICATION = "map_builder_invite";
    static final String BUILDER_REJECTED_NOTIFICATION = "map_builder_rejected";

    /// `deleted_reason` for an owner deleting their own unpublished map, the one case that needs
    /// no reason given.
    static final String USER_DELETION = "user_deletion";

    /// Go's search cache, cleared on every map write, and the verification leaderboard a reset
    /// throws away.
    static final String SEARCH_CACHE_PATTERN = "maps:search:*";
    static final String VERIFICATION_LEADERBOARD_PREFIX = "map:";
    static final String VERIFICATION_LEADERBOARD_SUFFIX = ":lb_playtime";

    private MapCompat() {}

    /// `stats` is null for an unpublished map, which has none.
    static MapData mapData(Maps map, List<String> tags, @Nullable MapStats stats) {
        long plays = stats == null ? 0 : stats.playCount();
        var winRate = plays == 0 ? 0.0 : (double) stats.winCount() / plays;
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
        var json = JsonParser.parseString(column).getAsJsonObject();
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

    static JsonObject notificationEvent(
        UUID playerId,
        String action,
        String type,
        String key,
        @Nullable JsonObject data
    ) {
        var event = new JsonObject();
        event.addProperty("playerId", playerId.toString());
        event.addProperty("action", action);
        event.addProperty("type", type);
        event.addProperty("key", key);
        event.add("data", data);
        return event;
    }
}
